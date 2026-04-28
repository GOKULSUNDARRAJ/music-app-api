const bcrypt = require('bcryptjs');
const { Op } = require('sequelize');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const { User, RecentlyPlayed, Category, Like, Song } = require('../models');

const JWT_SECRET = process.env.JWT_SECRET || 'music_app_secret_key';
const JWT_EXPIRES_IN = 31536000; // 1 year in seconds

// ─── Helper: generate tokens ──────────────────────────────────────────────────
const generateTokens = (userId) => {
  const access_token = jwt.sign(
    { sub: String(userId), scopes: ['basic', 'email'] },
    JWT_SECRET,
    { expiresIn: JWT_EXPIRES_IN }
  );
  const refresh_token = crypto.randomBytes(128).toString('hex');
  return { access_token, refresh_token };
};

const formatDate = (date) =>
  date
    ? new Date(date).toISOString().replace('T', ' ').slice(0, 19)
    : new Date().toISOString().replace('T', ' ').slice(0, 19);

// ─── Check Register ────────────────────────────────────────────────────────────
// POST /api/user/checkRegister  (form-data OR raw JSON)
exports.checkRegister = async (req, res, next) => {
  try {
    const {
      grant_type,
      client_id,
      userMobile,
      userCountry,
      name = '',
      deviceID = null,
      mobileType = null,
      device_token = null,
      referalCode = null
    } = req.body;

    if (!userMobile) {
      return res.status(400).json({
        status: false,
        error_type: '400',
        message: 'userMobile is required'
      });
    }

    const existing = await User.findOne({ where: { userMobile } });

    if (existing) {
      // Update device info
      await existing.update({
        deviceID: deviceID ?? existing.deviceID,
        mobileType: mobileType ?? existing.mobileType,
        device_token: device_token ?? existing.device_token
      });

      const { access_token, refresh_token } = generateTokens(existing.id);

      if (existing.isVerified) {
        // ── Verified user → full user info + error_type 200 ──
        return res.status(200).json({
          status: true,
          result: 'true',
          response: {
            userId: existing.id,
            userName: existing.userName,
            userEmail: existing.userEmail || '',
            userMobile: existing.userMobile,
            userCountry: existing.userCountry,
            userCountryId: existing.userCountryId,
            userCreatedDate: formatDate(existing.userCreatedDate),
            userCountryCode: existing.userCountryCode,
            token_type: 'Bearer',
            expires_in: JWT_EXPIRES_IN,
            access_token,
            refresh_token
          },
          message: 'You are successfully registered.',
          error_type: '200'
        });
      } else {
        // ── Not verified yet → resend verification prompt ──
        return res.status(200).json({
          status: true,
          result: 'true',
          response: {
            token_type: 'Bearer',
            expires_in: JWT_EXPIRES_IN - 1,
            access_token,
            refresh_token
          },
          message: 'You are successfully registered, Verify your mobile number',
          error_type: '202'
        });
      }
    }

    // ── New number → create user (unverified) + return tokens ──
    const newUser = await User.create({
      userName: name,
      userMobile,
      userCountryId: userCountry || '',
      userCountry: '',
      userCountryCode: '',
      deviceID,
      mobileType,
      device_token,
      referalCode,
      isVerified: false
    });

    const { access_token, refresh_token } = generateTokens(newUser.id);

    return res.status(200).json({
      status: true,
      result: 'true',
      response: {
        token_type: 'Bearer',
        expires_in: JWT_EXPIRES_IN - 1,
        access_token,
        refresh_token
      },
      message: 'You are successfully registered, Verify your mobile number',
      error_type: '202'
    });
  } catch (error) {
    return next(error);
  }
};

// ─── Number Verification ───────────────────────────────────────────────────────
// POST /api/user/secure/numberVerification OR /api/user/verifyOtp
// Header: Authorization: <token>  (requires valid JWT)
exports.numberVerification = async (req, res, next) => {
  try {
    const { verificationCode, otp } = req.body;
    const code = verificationCode || otp; // support both field names

    if (!code) {
      return res.status(400).json({
        status: false,
        error_type: '400',
        message: 'verificationCode/otp is required'
      });
    }

    const user = await User.findByPk(req.userId);

    if (!user) {
      return res.status(404).json({
        status: false,
        error_type: '404',
        message: 'User not found'
      });
    }

    if (user.isVerified) {
      return res.status(200).json({
        message: 'Mobile Number Already Verified.',
        error_type: '200',
        status: true
      });
    }

    // In a real app, we would verify the code here.
    // For now, any non-empty code marks the user as verified for testing.
    await user.update({ isVerified: true });

    return res.status(200).json({
      message: 'Mobile Number Verified Successfully.',
      error_type: '200',
      status: true
    });
  } catch (error) {
    return next(error);
  }
};

// ─── Resend Verification Code ──────────────────────────────────────────────────
// POST /api/user/resendVerificationCode
exports.resendVerificationCode = async (req, res, next) => {
  try {
    const user = await User.findByPk(req.userId);

    if (!user) {
      return res.status(404).json({
        status: false,
        error_type: '404',
        message: 'User not found'
      });
    }

    // In a real app, generate and send a new OTP here
    console.log(`[AUTH] Resending verification code for user: ${user.userMobile}`);

    return res.status(200).json({
      status: true,
      error_type: '200',
      message: 'Verification code sent successfully.'
    });
  } catch (error) {
    return next(error);
  }
};


// ─── Register (JSON) ──────────────────────────────────────────────────────────
exports.register = async (req, res, next) => {
  try {
    const {
      userName,
      userEmail,
      userPassword,
      userMobile,
      userCountry = '',
      userCountryId = '',
      userCountryCode = ''
    } = req.body;

    if (!userName || !userEmail || !userPassword || !userMobile) {
      return res.status(400).json({
        status: false,
        error_type: '400',
        message: 'userName, userEmail, userPassword and userMobile are required'
      });
    }

    const existing = await User.findOne({ where: { userEmail } });
    if (existing) {
      return res.status(409).json({
        status: false,
        error_type: '409',
        message: 'Email is already registered'
      });
    }

    const hashedPassword = await bcrypt.hash(userPassword, 10);

    const user = await User.create({
      userName,
      userEmail,
      userMobile,
      userPassword: hashedPassword,
      userCountry,
      userCountryId,
      userCountryCode
    });

    const { access_token, refresh_token } = generateTokens(user.id);

    return res.status(200).json({
      response: {
        userId: user.id,
        userName: user.userName,
        userEmail: user.userEmail,
        userMobile: user.userMobile,
        userCountry: user.userCountry,
        userCountryId: user.userCountryId,
        userCreatedDate: formatDate(user.userCreatedDate),
        userCountryCode: user.userCountryCode,
        token_type: 'Bearer',
        expires_in: JWT_EXPIRES_IN,
        access_token,
        refresh_token
      },
      message: 'You are successfully registered.',
      error_type: '200',
      status: true
    });
  } catch (error) {
    return next(error);
  }
};

// ─── Login ────────────────────────────────────────────────────────────────────
exports.login = async (req, res, next) => {
  try {
    const { userEmail, userPassword } = req.body;

    if (!userEmail || !userPassword) {
      return res.status(400).json({
        status: false,
        error_type: '400',
        message: 'userEmail and userPassword are required'
      });
    }

    const user = await User.findOne({ where: { userEmail } });
    if (!user) {
      return res.status(401).json({
        status: false,
        error_type: '401',
        message: 'Invalid email or password'
      });
    }

    const isMatch = await bcrypt.compare(userPassword, user.userPassword);
    if (!isMatch) {
      return res.status(401).json({
        status: false,
        error_type: '401',
        message: 'Invalid email or password'
      });
    }

    const { access_token, refresh_token } = generateTokens(user.id);

    return res.status(200).json({
      response: {
        userId: user.id,
        userName: user.userName,
        userEmail: user.userEmail,
        userMobile: user.userMobile,
        userCountry: user.userCountry,
        userCountryId: user.userCountryId,
        userCreatedDate: formatDate(user.userCreatedDate),
        userCountryCode: user.userCountryCode,
        token_type: 'Bearer',
        expires_in: JWT_EXPIRES_IN,
        access_token,
        refresh_token
      },
      message: 'You are successfully logged in.',
      error_type: '200',
      status: true
    });
  } catch (error) {
    return next(error);
  }
};

// ─── Get Profile (protected) ──────────────────────────────────────────────────
exports.getProfile = async (req, res, next) => {
  try {
    const user = await User.findByPk(req.userId, {
      attributes: { exclude: ['userPassword'] }
    });

    if (!user) {
      return res.status(404).json({
        status: false,
        error_type: '404',
        message: 'User not found'
      });
    }

    return res.status(200).json({
      response: {
        userId: user.id,
        userName: user.userName,
        userEmail: user.userEmail || '',
        userMobile: user.userMobile,
        userCountry: user.userCountry,
        userCountryId: user.userCountryId,
        userCreatedDate: formatDate(user.userCreatedDate),
        userCountryCode: user.userCountryCode
      },
      message: 'Profile fetched successfully.',
      error_type: '200',
      status: true
    });
  } catch (error) {
    return next(error);
  }
};

// ─── Get Terms and Conditions ─────────────────────────────────────────────────
// POST /api/user/getTermsAndConditions
exports.getTermsAndConditions = async (req, res) => {
  return res.status(200).json({
    result: 'true',
    error_type: '200',
    response: {
      title: 'Terms & Conditions',
      termsAndConditions: `  Saalai TV grants you to view and use the particular channel(s) and/or package of channels to which you have subscribed. Payment for particular channel(s) and/or channel Package subscriptions is made on a per channel/package per year or per channel/package per 6 Month basis. If you purchase a subscription to access a particular channel/channel bundles and cancel at any time during that subscription active period, Saalai TV will not refund any of your subscription charges for that particular year. It is required that you test your Internet Connection and Its Speed before subscribing to any Saalai TV Package. While you may cancel your subscription(s) at any time, Saalai TV will not provide any refund to users for reasons related in any way to their ability to receive a satisfactory video stream from Saalai TV Copyright. \r\n  \r\n  The Site Content and Site Code are owned by Saalai TV and/or its licensors and content providers and are protected by applicable domestic and international copyright laws. Unless expressly permitted elsewhere in the Site by Saalai TV, you shall not record, copy, distribute, publish, perform, modify, download, transmit, transfer, sell, license, reproduce, create derivative works from or based upon, distribute, post, publicly display, frame, link, or in any other way exploit any of the Site Content or Code, in whole or in part. Any rights not expressly granted to you are reserved. Any violation of copyright laws may result in severe civil and criminal penalties. Violators will be prosecuted to the maximum extent possible.\r\n  \r\n  USE OF THE SITE, GENERAL TERMS \r\n  \r\n  Saalai TV grants you a non-exclusive, non-transferable, limited right and license to access and use this Site, and view the Content subscribed to or purchased by you by way of one (1) computer connected to the Site over the Internet at any given time and in strict conformity with these Terms of Use.\r\n  \r\n  You agree to pay, using a valid credit card which Saalai TV accepts, the subscription charges set forth on the Site, applicable taxes, and other charges incurred on your account in order to access these services. Saalai TV reserves the right to increase fees, surcharges, and site subscription fees, or to institute new fees at any time, upon reasonable notice posted in advance on this Site. Ocean Media will not automatically charge your account for renewal of your yearly subscription. The renewal charge will be the same as the prior year charge, unless otherwise notified in advance.\r\n  \r\n  Non-Commercial/Personal Use Limitation. Saalai TV grants you this subscription for the limited purpose of personal use only, and not for any commercial purpose. No business entity (e.g., corporation, partnership, sole proprietorship) is licensed to use this Site, without prior written permission. You hereby agree not to use the Site, the materials, or any element or portion thereof (including, without limitation, e-mail addresses of users), for any commercial purpose whatsoever. For enterprise use, please contact our customer support team at: admin@saalaitv.com\r\n  \r\n  Nature of Services. Saalai TV offers its Site services for entertainment purposes only. Saalai TV does not warrant the truth or validity of the information contained on its site. You acknowledge and understand that, because of the possibility of human and mechanical error, mistakes or omissions in the data or information provided, delays or interruptions of the data or information stream from whatever cause, as well as other factors, Saalai TV is not responsible for errors in or omissions from the information contained or accessed through the Site.\r\n  \r\n  All information and content on the Site are provided on an "as is" and as available basis without warranty of any kind, express or implied, including, without limitation, the implied warranties of merchantability, fitness for a particular purpose, or non-infringement.\\r\\n \\r\\nNature of Content. Saalai TV offers video and audio content of various types including, but not limited to, sports, news, drama, etc. Saalai TV does not warrant that all content is appropriate for all age viewers. Minors should only access this Site with parental permission.\r\n  \r\n  You agree to submit Saalai TV with valid credit card information, which shall be used for collateral purpose only, and any other personal information requested for this purpose.\r\n  Username/Password. Saalai TV may issue you a username and/or password for the purpose of accessing the Content or certain portions thereof, or services provided via the Site. You shall hold and secure any such username or password as strictly confidential. Accordingly, you shall not allow friends, family, business associates or other person access to or use of such username or password. Saalai TV shall not be responsible whatsoever in the event that your password is misappropriated by a third party.\r\n  \r\n  Reservation of Rights. Any rights not expressly granted by Saalai TV herein are reserved. You may not download, modify, publish, transmit, transfer or sell, reproduce, create derivative works from or based on, distribute, perform, publicly display, or in any way exploit any of the Site Content, in whole or in part, without the prior written consent of a party authorized to bind Saalai TV, except as expressly permitted in these Terms of Use.\r\n  \r\n  Refund / Return Policy\r\n  If you are not 100% satisfied with your Product / Service, you can either return your order for a full refund or exchange it for something else. You can return or exchange your purchase for up to 7 days from the purchase date. Returned or exchanged products must be in the condition you received them and in the original box and/or packaging. We will Refund the Amount with Reduction of postage Charges.\r\n  \r\n  TERMS RELATING TO SUBSCRIPTION\r\n  \r\n  Terms relating to Subscription offerings.\r\n  Our Subscription offerings are available at different prices, as presented on our website and updated from time to time. We reserve the right to change those prices at any time. In any event, whatever the price, it will always be clearly marked on the subscription page for the channel/package/downloadable content and clearly marked when you enter your credit card information.\r\n  A valid credit card / Bank Transfer is required to pay for the Subscription offerings. The Subscription fee will be charged only when you submit the credit card information to us / when you Transfer the Amount to Our Bank Account.Once you subscribe to a particular video channel(s). You are limited to watching it on one Device.\r\n  \r\n  Terms relating to our Subscription Service.\r\n order to subscribe to our subscription service you will need to enter your credit card information. However, and this is important, WE WILL BILL YOUR CREDIT CARD FOR THE SUBSCRIPTION FEE.\r\n  \r\n  SECURITY POLICY CHILDREN\\r\\n \\r\\nWe do not solicit or knowingly accept information from persons under the age of 18.\r\n  \r\n  By using this web site, you consent to the terms of our Privacy Policy and to Saalai TV's processing of personal information for the purposes set forth above. Should the Privacy Policy change, we intend to take every reasonable step to ensure that these changes are brought to your attention by posting all changes prominently on our Site for a reasonable period of time.\r\n  \r\n  DISCLAIMER OF WARRANTY; LIMITATION OF LIABILITY\r\n  \r\n  As with any on-line interaction and electronic communication, there is an inherent risk involved in transmitting any information via the Internet. Saalai TV does not and cannot guarantee that its system is free from hackers or viruses or that information provided by users to Saalai TV will not be stolen or otherwise surreptitiously obtained. Saalai TV is not responsible or liable for any infections or contamination of your system or delays, inaccuracies, errors, or omissions arising out of your use of this Site or with respect to the materials contained on this Site. You hereby acknowledge and understand that such risk is inherent in interacting with any website, including this Site, and take full responsibility for any harm, danger or damage that result due to any such breach in security. Pursuant to these Terms of Use, Saalai TV expressly disclaims any such liability. In addition, you agree to be responsible for obtaining and maintaining all telephone, computer hardware and other equipment needed for access to and use of this Site and you shall be solely responsible for all charges related thereto and operations thereof.\\r\\n \\r\\nNeither Saalai TV nor any provider of content for the site or their respective agents warrant that the site will be uninterrupted or error free, free from viruses or security breaches; nor does Saalai TV, any provider of content to the site, or their respective agents make any warranty as to the results to be obtained from use of the site. The site content is not guaranteed to be accurate, timely or verified. The Site and the Site's content are distributed on an "as is" and "as available" basis. Saalai TV and its agents do not make any warranties of any kind, either express or implied, including, without limitation, warranties of title or warranties of merchantability or fitness for a particular purpose, with respect to the site, any content, or any goods or services sold through the site. You expressly agree that the entire risk as to the quality and performance of the site and the accuracy or completeness of the content or data made available via the Site is assumed solely by you.\r\n  \r\n  Neither Saalai TV nor their respective agents shall be liable for any direct, indirect, incidental, special or consequential damages arising out of the use of or inability to use the Site.\r\n  Without waiving the exclusive governing law clause below, some jurisdictions do not allow exclusion of implied warranties or limitations of liability for incidental or consequential damages, so the above limitations or exclusions may not apply to you. In such jurisdictions, the liability of Saalai TV, third party content providers, and their respective agents shall be limited to the greatest extent permitted by law.\r\n  \r\n  Links to Other Web Sites. Saalai TV makes no representation whatsoever regarding the content of any other web sites which you may access from the Saalai TV web Site. When you access a web site other than the Site, please understand that it is independent from Saalai TV and that Saalai TV has no control over the content on that web site. A link to a web site other than the Site does not mean that Saalai TV endorses or accepts any responsibility for the content or use of such web site.\r\n  \r\n  MISCELLANEOUS\r\n  \r\n  Notices. Notices by Saalai TV to you may be given by means of a general posting at the Site. Notices (including questions, complaints, or legal notices) by you may be given by electronic messages or conventional mail, unless otherwise specified in these Terms of Use as follows:\r\n  Electronic mail (e-mail) must be sent to: admin@saalaitv.com\r\n  Modifications. Saalai TV has the right to modify this Agreement and any policies affecting the Site. Any modification is effective immediately upon posting to the Site or distribution via electronic mail or conventional mail. Your continued use of the Site following notice of any modification to this Agreement shall be conclusively deemed an acceptance of all such modification(s). Your only right with respect to any dissatisfaction with any modifications made pursuant to this provision, or any policies or practices of Saalai TV in providing the Site, including without limitation (i) any change in the Content, or (ii) any change in the amount or type of fees associated with the Fee-Based Services, is to cancel your subscription in accordance with the subscription help instructions. Ocean Media has the right to modify, suspend or discontinue the Site or any portion thereof at any time, including the availability of any area of the Site, including without limitation the Fee-Based Services. Saalai TV may also impose limits on certain features and services or restrict your access to parts or all of the Saalai TV Site without notice or liability.`
    }
  });
};

// ─── Logout ───────────────────────────────────────────────────────────────────
// POST /api/user/logout
exports.logout = async (req, res) => {
  return res.status(200).json({
    status: true,
    message: 'Logged out successfully'
  });
};

// ─── Update Profile (protected) ───────────────────────────────────────────────
// PUT /api/user/profile
exports.updateProfile = async (req, res, next) => {
  try {
    const { userName, userEmail, userCountry, userCountryId, userCountryCode } = req.body;

    const user = await User.findByPk(req.userId);

    if (!user) {
      return res.status(404).json({
        status: false,
        error_type: '404',
        message: 'User not found'
      });
    }

    if (userEmail && userEmail !== user.userEmail) {
      const existing = await User.findOne({ where: { userEmail } });
      if (existing) {
        return res.status(409).json({
          status: false,
          error_type: '409',
          message: 'Email is already taken'
        });
      }
    }

    await user.update({
      userName: userName ?? user.userName,
      userEmail: userEmail ?? user.userEmail,
      userCountry: userCountry ?? user.userCountry,
      userCountryId: userCountryId ?? user.userCountryId,
      userCountryCode: userCountryCode ?? user.userCountryCode
    });

    return res.status(200).json({
      response: {
        userId: user.id,
        userName: user.userName,
        userEmail: user.userEmail || '',
        userMobile: user.userMobile,
        userCountry: user.userCountry,
        userCountryId: user.userCountryId,
        userCreatedDate: formatDate(user.userCreatedDate),
        userCountryCode: user.userCountryCode
      },
      message: 'Profile updated successfully.',
      error_type: '200',
      status: true
    });
  } catch (error) {
    return next(error);
  }
};

// ─── Delete Account (protected) ───────────────────────────────────────────────
// DELETE /api/user/account
exports.deleteAccount = async (req, res, next) => {
  try {
    const user = await User.findByPk(req.userId);

    if (!user) {
      return res.status(404).json({
        status: false,
        error_type: '404',
        message: 'User not found'
      });
    }

    await user.destroy();

    return res.status(200).json({
      status: true,
      error_type: '200',
      message: 'Account deleted successfully'
    });
  } catch (error) {
    return next(error);
  }
};

// ─── Record Recent Play (protected) ──────────────────────────────────────────
// POST /api/user/recordRecentPlay
exports.recordRecentPlay = async (req, res, next) => {
  try {
    let { categoryId } = req.body;

    if (!categoryId) {
      return res.status(400).json({ message: 'categoryId is required' });
    }

    // Handle formatted IDs like "cat_001"
    if (typeof categoryId === 'string' && categoryId.startsWith('cat_')) {
      categoryId = parseInt(categoryId.split('_')[1], 10);
    }

    // Verify category exists
    const category = await Category.findByPk(categoryId);
    if (!category) {
      return res.status(404).json({ message: 'Category not found' });
    }


    // Find or create record
    const [record, created] = await RecentlyPlayed.findOrCreate({
      where: { userId: req.userId, categoryId },
      defaults: { playedAt: new Date() }
    });

    if (!created) {
      // If it exists, update the timestamp
      await record.update({ playedAt: new Date() });
    }

    return res.status(200).json({
      status: true,
      message: 'Play recorded successfully'
    });
  } catch (error) {
    return next(error);
  }
};

// ─── Toggle Like (protected) ────────────────────────────────────────────────
// POST /api/user/toggleLike
exports.toggleLike = async (req, res, next) => {
  try {
    let { categoryId, songId } = req.body;
    const userId = req.userId;

    if (!categoryId && !songId) {
      return res.status(400).json({ status: false, message: 'categoryId or songId is required' });
    }

    const where = { userId };
    if (categoryId) {
      if (typeof categoryId === 'string' && categoryId.startsWith('cat_')) {
        categoryId = parseInt(categoryId.split('_')[1], 10);
      }
      where.categoryId = categoryId;
    } else {
      if (typeof songId === 'string' && songId.startsWith('song_')) {
        songId = parseInt(songId.split('_')[1], 10);
      }
      where.songId = songId;
    }

    const existing = await Like.findOne({ where });

    if (existing) {
      await existing.destroy();
      return res.json({ status: true, message: 'Unliked successfully', isLiked: false, liked: false });
    } else {
      await Like.create({ userId, categoryId, songId });
      return res.json({ status: true, message: 'Liked successfully', isLiked: true, liked: true });
    }
  } catch (err) {
    return next(err);
  }
};

// ─── Get Liked Items (protected) ─────────────────────────────────────────────
// GET /api/user/liked-items
exports.getLikedItems = async (req, res, next) => {
  try {
    const userId = req.userId;

    const likes = await Like.findAll({
      where: { userId },
      include: [
        { model: Category, as: 'category', include: [{ model: Song, as: 'songs' }] },
        { model: Song, as: 'song' }
      ]
    });

    // Format IDs for response consistency
    const formatEntityId = (prefix, id) => `${prefix}_${String(id).padStart(3, '0')}`;

    const likedCategories = likes
      .filter(l => l.categoryId)
      .map(l => ({
        categoryId: formatEntityId('cat', l.category.id),
        categoryName: l.category.categoryName,
        categoryImage: l.category.categoryImage,
        adapterType: l.category.adapterType,
        songs: (l.category.songs || []).map(s => ({
          songId: formatEntityId('song', s.id),
          audioName: s.audioName,
          audioUrl: s.audioUrl,
          imageUrl: s.imageUrl,
          categoryId: formatEntityId('cat', s.categoryId)
        }))
      }));

    const likedSongs = likes
      .filter(l => l.songId)
      .map(l => ({
        songId: formatEntityId('song', l.song.id),
        audioName: l.song.audioName,
        audioUrl: l.song.audioUrl,
        imageUrl: l.song.imageUrl,
        categoryId: formatEntityId('cat', l.song.categoryId)
      }));

    return res.json({
      status: true,
      likedCategories,
      likedSongs
    });
  } catch (err) {
    return next(err);
  }
};

// ─── Check Category Liked (protected) ──────────────────────────────────────
// GET /api/user/liked-items/category/:categoryId
exports.checkCategoryLiked = async (req, res, next) => {
  try {
    let { categoryId } = req.params;
    const userId = req.userId;

    if (typeof categoryId === 'string' && categoryId.startsWith('cat_')) {
      categoryId = parseInt(categoryId.split('_')[1], 10);
    }

    const existing = await Like.findOne({ where: { userId, categoryId } });
    return res.json({ 
      status: true, 
      liked: !!existing, 
      isLiked: !!existing 
    });
  } catch (err) {
    return next(err);
  }
};

// ─── Get Liked Playlist (Sections format for Android) ─────────────────────
// GET /api/user/liked-playlist
exports.getLikedPlaylist = async (req, res, next) => {
  try {
    const userId = req.userId;

    const likes = await Like.findAll({
      where: { userId, categoryId: { [Op.ne]: null } },
      include: [
        { 
          model: Category, 
          as: 'category', 
          include: [{ model: Song, as: 'songs' }] 
        }
      ]
    });

    const formatEntityId = (prefix, id) => `${prefix}_${String(id).padStart(3, '0')}`;

    const categories = likes
      .filter(l => l.category) // Ensure category exists
      .map(l => ({
        categoryId: formatEntityId('cat', l.category.id),
        categoryName: l.category.categoryName,
        categoryImage: l.category.categoryImage,
        adapterType: 5,
        isLiked: true,
        liked: true,
        songs: (l.category.songs || []).map(s => ({
          songId: formatEntityId('song', s.id),
          audioName: s.audioName,
          audioUrl: s.audioUrl,
          imageUrl: s.imageUrl,
          categoryId: formatEntityId('cat', s.categoryId),
          isLiked: true,
          liked: true
        }))
      }));

    return res.json({
      status: true,
      result: "true",
      sections: [
        {
          sectionId: "sec_library",
          sectionTitle: "My Library",
          sectionName: "My Library",
          layoutType: 4,
          spanCount: 4,
          artistCategories: categories,
          artists: categories,
          categories: categories
        }
      ]
    });
  } catch (err) {
    return next(err);
  }
};
