package com.example.smartessay.CreatingAccounts;

import java.util.Random;

public class OTPverification {

    public static String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // ensures 6-digit OTP
        return String.valueOf(otp);
    }
}
