package com.github.mmauro.glhmg;

import org.jetbrains.annotations.Nullable;

public class Param<T> {

	public interface Validator<T> {
		void validate(T value) throws IllegalArgumentException;
	}

	@Nullable
	private final Validator<T> validator;
	private T value;
	private boolean hasValue = false;

	public Param() {
		this(null);
	}

	public Param(@Nullable Validator<T> validator) {
		this.validator = validator;
	}

	public void setValue(T value) {
		if (validator != null) {
			validator.validate(value);
		}
		this.value = value;
		this.hasValue = true;
	}

	public void removeValue() {
		this.value = null;
		this.hasValue = false;
	}

	public boolean hasValue() {
		return hasValue;
	}

	public T getValue() {
		if (!hasValue) {
			throw new IllegalStateException("value not set");
		}
		return value;
	}

	public boolean isNull() {
		return getValue() == null;
	}

	public T optValue() {
		return optValue(null);
	}

	public T optValue(T defaultValue) {
		if (!hasValue) {
			return defaultValue;
		} else {
			return value;
		}
	}
}
