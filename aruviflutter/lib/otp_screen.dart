import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'dart:async';
import 'main_activity.dart';
import 'sign_up_screen.dart';

class OtpScreen extends StatefulWidget {
  const OtpScreen({super.key});

  @override
  State<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends State<OtpScreen> {
  final List<TextEditingController> _otpControllers = List.generate(4, (_) => TextEditingController());
  final List<FocusNode> _focusNodes = List.generate(4, (_) => FocusNode());

  bool _isVerifyEnabled = false;
  bool _isVerifying = false;
  bool _isResending = false;

  int _resendSeconds = 60;
  Timer? _timer;
  String _userMobile = "";

  @override
  void initState() {
    super.initState();
    _loadUserMobile();
    _startTimer();
  }

  Future<void> _loadUserMobile() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _userMobile = prefs.getString('userMobile') ?? "";
    });
  }

  void _startTimer() {
    setState(() {
      _resendSeconds = 60;
    });
    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_resendSeconds > 0) {
        setState(() {
          _resendSeconds--;
        });
      } else {
        timer.cancel();
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    for (var c in _otpControllers) {
      c.dispose();
    }
    for (var f in _focusNodes) {
      f.dispose();
    }
    super.dispose();
  }

  void _onOtpChanged(String value, int index) {
    if (value.isNotEmpty && index < 3) {
      _focusNodes[index + 1].requestFocus();
    } else if (value.isEmpty && index > 0) {
      _focusNodes[index - 1].requestFocus();
    }
    _checkVerifyEnabled();
  }

  void _checkVerifyEnabled() {
    bool allFilled = _otpControllers.every((c) => c.text.trim().isNotEmpty);
    setState(() {
      _isVerifyEnabled = allFilled;
    });
  }

  Future<void> _resendOtp() async {
    if (_resendSeconds > 0 || _isResending) return;

    setState(() {
      _isResending = true;
    });

    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('access_token') ?? '';

      final response = await http.post(
        Uri.parse('https://music-app-api-1.onrender.com/api/secure/reSendVerificationCode'),
        headers: {
          'Authorization': token,
        },
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        if (data['status'] == true) {
          if (!mounted) return;
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('OTP sent successfully!')),
          );
          _startTimer();
          for (var c in _otpControllers) {
             c.clear();
          }
          _checkVerifyEnabled();
          _focusNodes[0].requestFocus();
        } else {
          if (!mounted) return;
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(data['message'] ?? 'Failed to resend OTP')),
          );
        }
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Network error: $e')),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isResending = false;
        });
      }
    }
  }

  Future<void> _verifyOtp() async {
    if (!_isVerifyEnabled || _isVerifying) return;

    setState(() {
      _isVerifying = true;
    });

    String otpCode = _otpControllers.map((c) => c.text).join('');

    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('access_token') ?? '';

      final response = await http.post(
        Uri.parse('https://music-app-api-1.onrender.com/api/secure/numberVerification'),
        headers: {
          'Authorization': token,
        },
        body: {
          'verificationCode': otpCode,
        },
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        if (data['status'] == true && data['error_type'] == "200") {
          if (!mounted) return;
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(data['message'] ?? 'Verified successfully')),
          );
          Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const MainActivity()));
        } else {
          if (!mounted) return;
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(data['message'] ?? 'Verification failed')),
          );
          for (var c in _otpControllers) {
             c.clear();
          }
          _checkVerifyEnabled();
          _focusNodes[0].requestFocus();
        }
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Network error: $e')),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isVerifying = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF181A20), // darkGray
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(10.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              const SizedBox(height: 40),
              // Header with Back Button
              Stack(
                alignment: Alignment.center,
                children: [
                  Align(
                    alignment: Alignment.centerLeft,
                    child: IconButton(
                      icon: const Icon(Icons.arrow_back, color: Colors.white, size: 30),
                      onPressed: () {
                        Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const SignUpScreen()));
                      },
                    ),
                  ),
                  const Text(
                    'OTP Verification',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 20, // channel_name_text_size
                      fontWeight: FontWeight.w500, // circularstdmedium
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 50),

              // Verification Message
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20.0),
                child: Text(
                  _userMobile.isNotEmpty 
                      ? "We have sent a verification code on +$_userMobile"
                      : "We have sent a verification code on your registered number",
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 20, // 20dp
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
              const SizedBox(height: 25),

              // OTP Input Boxes
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(4, (index) {
                  return Container(
                    margin: const EdgeInsets.symmetric(horizontal: 4.0),
                    width: 55,
                    height: 55,
                    decoration: BoxDecoration(
                      color: const Color(0xFF1F222A), // otp_box_bg color
                      borderRadius: BorderRadius.circular(4),
                      border: Border.all(color: const Color(0xFF1F222A), width: 1), // matching stroke
                    ),
                    child: TextField(
                      controller: _otpControllers[index],
                      focusNode: _focusNodes[index],
                      keyboardType: TextInputType.number,
                      textAlign: TextAlign.center,
                      maxLength: 1,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 18,
                        fontWeight: FontWeight.bold, // circularstdbold
                      ),
                      decoration: const InputDecoration(
                        counterText: "",
                        border: InputBorder.none,
                      ),
                      onChanged: (value) => _onOtpChanged(value, index),
                      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                    ),
                  );
                }),
              ),
              const SizedBox(height: 30),

              // Didn't receive OTP text
              const Text(
                "Don't receive OTP ?",
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 17,
                  fontWeight: FontWeight.w500,
                ),
              ),
              const SizedBox(height: 10),

              // Resend Text
              GestureDetector(
                onTap: _resendOtp,
                child: _isResending
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2),
                      )
                    : RichText(
                        text: TextSpan(
                          style: const TextStyle(
                            fontSize: 17,
                            fontWeight: FontWeight.w500,
                          ),
                          children: [
                            TextSpan(
                              text: "Didn't receive OTP? ",
                              style: TextStyle(
                                color: _resendSeconds == 0 ? Colors.white : Colors.white,
                              ),
                            ),
                            TextSpan(
                              text: _resendSeconds == 0 ? "Resend" : "Resend in ${_resendSeconds}s",
                              style: TextStyle(
                                color: _resendSeconds == 0 
                                    ? const Color(0xFFEB1C24) // bgred
                                    : Colors.grey, // gray
                              ),
                            ),
                          ],
                        ),
                      ),
              ),
              
              const SizedBox(height: 100), // marginBottom 100dp matching Android constraint

              // Verify Button
              InkWell(
                onTap: _verifyOtp,
                borderRadius: BorderRadius.circular(100),
                child: Opacity(
                  opacity: _isVerifyEnabled && !_isVerifying ? 1.0 : 0.5,
                  child: Container(
                    width: 350,
                    height: 45,
                    decoration: BoxDecoration(
                      color: const Color(0xFFEB1C24), // onboardbtnbg
                      borderRadius: BorderRadius.circular(100),
                    ),
                    child: Stack(
                      alignment: Alignment.center,
                      children: [
                        if (!_isVerifying)
                          Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: const [
                              Text(
                                'Verify',
                                style: TextStyle(
                                  fontSize: 24, // 24sp
                                  fontWeight: FontWeight.w500,
                                  color: Colors.white,
                                ),
                              ),
                              SizedBox(width: 5),
                              Icon(Icons.arrow_forward, color: Colors.white),
                            ],
                          ),
                        if (_isVerifying)
                          const SizedBox(
                            width: 25,
                            height: 25,
                            child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2),
                          ),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
