package top.lyh.validatio;

import cn.hutool.core.util.IdcardUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.ObjectUtils;
import top.lyh.utils.ResetMessageUtil;

/**
 * 身份证号码，格式校验器
 */
public class IdCardValidator implements ConstraintValidator<IdCard, String> {

    @Override
    public void initialize(IdCard constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }


    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (ObjectUtils.isEmpty(value)) {
            return true;
        }

        if (value.contains(" ")) {
            ResetMessageUtil.reset(context, "身份证号码错误！");
            return false;
        }
        //糊涂包的身份证号码校验方法
        return IdcardUtil.isValidCard(value);
    }

}

