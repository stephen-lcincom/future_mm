package io.darasa.futuremm.validator;

import java.util.List;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EnumValidatorConstraint implements ConstraintValidator<EnumValidator, String> {

  private List<String> values;
  private boolean nullable;

  @Override
  public void initialize(EnumValidator annotation) {
    values = Helper.getEnumValues(annotation.clazz());
    nullable = annotation.nullable();
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null && nullable) {
      return true;
    }

    return values.contains(value);
  }
}