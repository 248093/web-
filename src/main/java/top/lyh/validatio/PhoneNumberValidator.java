package top.lyh.validatio;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import top.lyh.utils.ResetMessageUtil;

import java.util.regex.Pattern;

public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {
    
    /**
     * 手机号正则表达式
     * 支持中国大陆手机号码格式
     */
    private static final String PHONE_PATTERN = 
        "^1(3[0-9]|4[01456879]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[0-35-9])\\d{8}$";
    
    private static final Pattern PATTERN = Pattern.compile(PHONE_PATTERN);
    
    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            ResetMessageUtil.reset(context, "手机号码错误！");
            return false;
        }
        return PATTERN.matcher(phoneNumber).matches();
    }
}
