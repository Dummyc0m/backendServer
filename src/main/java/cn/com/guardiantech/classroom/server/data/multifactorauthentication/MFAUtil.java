package cn.com.guardiantech.classroom.server.data.multifactorauthentication;

import com.google.common.io.BaseEncoding;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by Codetector on 2016/12/26.
 */
public class MFAUtil {
    /** default time-step which is part of the spec, 30 seconds is default */
    public static final int DEFAULT_TIME_STEP_SECONDS = 30;
    /** set to the number of digits to control 0 prefix, set to 0 for no prefix */
    private static int NUM_DIGITS_OUTPUT = 6;

    private static final String blockOfZeros;

    static {
        char[] chars = new char[NUM_DIGITS_OUTPUT];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = '0';
        }
        blockOfZeros = new String(chars);
    }

    /**
     * Generate and return a secret key in base32 format (A-Z2-7) using {@link SecureRandom}. Could be used to generate
     * the QR image to be shared with the user.
     */
    public static String generateBase32Secret() {
        StringBuilder sb = new StringBuilder();
        Random random = new SecureRandom();
        for (int i = 0; i < 16; i++) {
            int val = random.nextInt(32);
            if (val < 26) {
                sb.append((char) ('A' + val));
            } else {
                sb.append((char) ('2' + (val - 26)));
            }
        }
        return sb.toString();
    }

    /**
     * Return the current number to be checked. This can be compared against user input.
     *
     * <p>
     * WARNING: This requires a system clock that is in sync with the world.
     * </p>
     *
     * <p>
     * For more details of this magic algorithm, see:
     * http://en.wikipedia.org/wiki/Time-based_One-time_Password_Algorithm
     * </p>
     *
     * @param secret
     *            Secret string that was used to generate the QR code or shared with the user.
     */
    public static String generateCurrentNumber(String secret) {
        try {
            return generateCurrentNumber(secret, System.currentTimeMillis(), DEFAULT_TIME_STEP_SECONDS);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Same as {@link #generateCurrentNumber(String)} except exposes other parameters.
     *
     * @param secret
     *            Secret string that was used to generate the QR code or shared with the user.
     * @param currentTimeMillis
     *            Current time in milliseconds.
     * @param timeStepSeconds
     *            Time step in seconds. The default value is 30 seconds here. See {@link #DEFAULT_TIME_STEP_SECONDS}.
     */
    public static String generateCurrentNumber(String secret, long currentTimeMillis, int timeStepSeconds)
            throws GeneralSecurityException {

        byte[] key = BaseEncoding.base32().decode(secret);

        byte[] data = new byte[8];
        long value = currentTimeMillis / 1000 / timeStepSeconds;
        for (int i = 7; value > 0; i--) {
            data[i] = (byte) (value & 0xFF);
            value >>= 8;
        }

        // encrypt the data with the key and return the SHA1 of it in hex
        SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
        // if this is expensive, could put in a thread-local
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);

        // take the 4 least significant bits from the encrypted string as an offset
        int offset = hash[hash.length - 1] & 0xF;

        // We're using a long because Java hasn't got unsigned int.
        long truncatedHash = 0;
        for (int i = offset; i < offset + 4; ++i) {
            truncatedHash <<= 8;
            // get the 4 bytes at the offset
            truncatedHash |= (hash[i] & 0xFF);
        }
        // cut off the top bit
        truncatedHash &= 0x7FFFFFFF;

        // the token is then the last 6 digits in the number
        truncatedHash %= 1000000;

        return zeroPrepend(truncatedHash, NUM_DIGITS_OUTPUT);
    }

    /**
     * Return the string prepended with 0s. Tested as 10x faster than String.format("%06d", ...); Exposed for testing.
     */
    static String zeroPrepend(long num, int digits) {
        String numStr = Long.toString(num);
        if (numStr.length() >= digits) {
            return numStr;
        } else {
            StringBuilder sb = new StringBuilder(digits);
            int zeroCount = digits - numStr.length();
            sb.append(blockOfZeros, 0, zeroCount);
            sb.append(numStr);
            return sb.toString();
        }
    }
}
