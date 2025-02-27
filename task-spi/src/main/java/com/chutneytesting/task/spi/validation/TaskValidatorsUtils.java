package com.chutneytesting.task.spi.validation;

import static com.chutneytesting.task.spi.validation.Validator.of;

import com.chutneytesting.task.spi.injectable.Target;
import com.chutneytesting.task.spi.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

public class TaskValidatorsUtils {

    public static Validator<Target> targetValidation(Target target) {
        return of(target)
            .validate(Objects::nonNull, "No target provided")
            .validate(Target::name, StringUtils::isNotBlank, "Target name is blank")
            .validate(Target::url, StringUtils::isNotBlank, "No url defined on the target")
            .validate(Target::getUrlAsURI, noException -> true, "Target url is not valid")
            .validate(Target::getUrlAsURI, uri -> uri.getHost() != null && !uri.getHost().isEmpty(), "Target url has an undefined host");
    }

    public static Validator<Target> targetPropertiesNotBlankValidation(Target target, String... properties) {
        Validator<Target> targetValidator = of(target);
        Arrays.stream(properties).forEach(p -> targetValidator.validate(t -> t.properties().get(p), StringUtils::isNotBlank, "Target property [" + p + "] is blank"));
        return targetValidator;
    }

    public static Validator<String> durationValidation(String duration, String inputLabel) {
        return of(duration)
            .validate(StringUtils::isNotBlank, "No " + inputLabel + " provided")
            .validate(Duration::parseToMs, noException -> true, inputLabel + " is not parsable");
    }

    public static <T> Validator<List<T>> notEmptyListValidation(List<T> toVerify, String inputLabel) {
        return of(toVerify)
            .validate(Objects::nonNull, "No " + inputLabel + " provided (List)")
            .validate(m -> !m.isEmpty(), inputLabel + " should not be empty");
    }

    public static <K, V> Validator<Map<K, V>> notEmptyMapValidation(Map<K, V> toVerify, String inputLabel) {
        return of(toVerify)
            .validate(Objects::nonNull, "No " + inputLabel + " provided (Map)")
            .validate(m -> !m.isEmpty(), inputLabel + " should not be empty");
    }

    public static Validator<String> notBlankStringValidation(String toVerify, String inputLabel) {
        return of(toVerify)
            .validate(Objects::nonNull, "No " + inputLabel + " provided (String)")
            .validate(StringUtils::isNotBlank, inputLabel + " should not be blank");
    }
}
