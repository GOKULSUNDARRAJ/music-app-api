import 'package:flutter/material.dart';
import 'package:flutter/gestures.dart';
import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'main_activity.dart'; 
import 'otp_screen.dart';

class SignUpScreen extends StatefulWidget {
  const SignUpScreen({super.key});

  @override
  State<SignUpScreen> createState() => _SignUpScreenState();
}

class _SignUpScreenState extends State<SignUpScreen> {
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _mobileController = TextEditingController();
  final TextEditingController _referralController = TextEditingController();
  bool _agreedToTerms = false;
  String _selectedCountryCode = '+235';
  String _selectedCountryName = 'United Kingdom';

  Future<void> _submit() async {
    if (_nameController.text.isEmpty || _mobileController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please fill all required fields')),
      );
      return;
    }
    if (!_agreedToTerms) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please agree to Terms & Privacy Policy')),
      );
      return;
    }

    try {
      final response = await http.post(
        Uri.parse('https://music-app-api-1.onrender.com/api/checkRegister'),
        body: {
          "grant_type": "password",
          "client_id": "saalai_app",
          "userCountry": "228", // Using the Android code default value
          "userMobile": _mobileController.text,
          "deviceID": "a43c5951b27652d123", // Placeholder device ID
          "mobileType": "A",
          "deviceToken": "your_device_token_here",
          "name": _nameController.text,
          "referalCode": _referralController.text,
        },
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        if (data['status'] == true) {
          // Success
          if (data['error_type'] == "200") {
             // User exists, tokens are returned
             final prefs = await SharedPreferences.getInstance();
             await prefs.setString('access_token', data['response']['access_token']);
             await prefs.setString('userMobile', _mobileController.text);
             if (!mounted) return;
             Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const MainActivity()));
          } else {
             // New user, need OTP
             final prefs = await SharedPreferences.getInstance();
             if (data['response'] != null && data['response']['access_token'] != null) {
               await prefs.setString('access_token', data['response']['access_token']);
             }
             await prefs.setString('userMobile', _mobileController.text);
             
             if (!mounted) return;
             Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const OtpScreen()));
          }
        } else {
          if (!mounted) return;
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(data['message'] ?? 'Registration failed')),
          );
        }
      } else {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Server error occurred')),
        );
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Network error: $e')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF181A20), // darkGray
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 10.0, vertical: 10.0), // padding 10dp
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const SizedBox(height: 50), // layout_marginTop 50dp
              // Logo
              Container(
                width: 350,
                height: 150,
                padding: const EdgeInsets.all(20.0), // padding 20dp
                child: Image.asset(
                  'assets/images/arivumusiclogo.png',
                  fit: BoxFit.contain,
                ),
              ),
              const SizedBox(height: 20),

              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 10.0), // 350dp width container in Android
                child: Column(
                  children: [
                    _buildTextField('Name', _nameController),
                    const SizedBox(height: 20),
                    
                    // Country Code
                    Container(
                      padding: const EdgeInsets.all(13.0),
                      decoration: BoxDecoration(
                        color: const Color(0xFF1F222A), // lightGray
                        borderRadius: BorderRadius.circular(5),
                      ),
                      child: Row(
                        children: [
                          Text(
                            _selectedCountryCode,
                            style: const TextStyle(color: Color(0xFF828993), fontSize: 16), // gray1
                          ),
                          const SizedBox(width: 10),
                          Text(
                            _selectedCountryName,
                            style: const TextStyle(color: Color(0xFF828993), fontSize: 16),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 20),

                    _buildTextField('Mobile Number', _mobileController, keyboardType: TextInputType.phone),
                    const SizedBox(height: 20),

                    _buildTextField('Referral Code (optional)', _referralController),
                    const SizedBox(height: 20),

                    // Checkbox and Terms
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        SizedBox(
                          width: 24,
                          height: 24,
                          child: Checkbox(
                            value: _agreedToTerms,
                            onChanged: (val) {
                              setState(() {
                                _agreedToTerms = val ?? false;
                              });
                            },
                            activeColor: const Color(0xFFEB1C24),
                            checkColor: Colors.white,
                            side: const BorderSide(color: Color(0xFF828993)),
                          ),
                        ),
                        const SizedBox(width: 5), // marginLeft 5dp
                        Expanded(
                          child: Padding(
                            padding: const EdgeInsets.only(top: 2.0), // rough padding adjustment
                            child: RichText(
                              text: TextSpan(
                                style: const TextStyle(
                                  color: Color(0xFF828993), // gray1
                                  fontSize: 13, // 13sp
                                  height: 1.3, // lineSpacingExtra 4dp
                                  fontWeight: FontWeight.bold,
                                ),
                                children: [
                                  const TextSpan(text: 'By entering your number, you are agreeing to our '),
                                  TextSpan(
                                    text: 'Terms and Conditions',
                                    style: const TextStyle(color: Color(0xFF828993), fontWeight: FontWeight.bold),
                                    recognizer: TapGestureRecognizer()..onTap = () {},
                                  ),
                                  const TextSpan(text: ' and '),
                                  TextSpan(
                                    text: 'Privacy Policy',
                                    style: const TextStyle(color: Color(0xFF828993), fontWeight: FontWeight.bold),
                                    recognizer: TapGestureRecognizer()..onTap = () {},
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 20),

                    // Submit Button
                    InkWell(
                      onTap: _submit,
                      borderRadius: BorderRadius.circular(100),
                      child: Container(
                        width: double.infinity,
                        height: 45, // 45dp
                        decoration: BoxDecoration(
                          color: const Color(0xFFEB1C24), // @drawable/onboardbtnbg
                          borderRadius: BorderRadius.circular(100),
                        ),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: const [
                            Text(
                              'Submit',
                              style: TextStyle(
                                fontSize: 24, // 24sp
                                fontWeight: FontWeight.w500, // circularstdmedium
                                color: Colors.white,
                              ),
                            ),
                            SizedBox(width: 5),
                            Icon(Icons.arrow_forward, color: Colors.white),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTextField(String hint, TextEditingController controller, {TextInputType keyboardType = TextInputType.text}) {
    return TextField(
      controller: controller,
      keyboardType: keyboardType,
      style: const TextStyle(color: Colors.white),
      decoration: InputDecoration(
        hintText: hint,
        hintStyle: const TextStyle(color: Color(0xFF828993)), // gray1
        filled: true,
        fillColor: const Color(0xFF1F222A), // lightGray
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(5),
          borderSide: BorderSide.none,
        ),
        contentPadding: const EdgeInsets.all(13.0), // padding 13dp
      ),
    );
  }
}
