/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.account.data;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.UnsupportedParameterException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.account.AccountDetailConstants;
import org.apache.fineract.portfolio.account.api.StandingInstructionApiConstants;
import org.apache.fineract.portfolio.account.data.AccountTransfersDetailDataValidator;
import org.apache.fineract.portfolio.account.data.StandingInstructionDataValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class StandingInstructionDataValidatorTest {
    private static final String STANDING_INSTRUCTION_RESOURCE_NAME_PREFIX = "validation.msg.standinginstruction.";
    private static final String CANNOT_BE_BLANK_ERROR_CODE = "cannot.be.blank";
    private static final String OUT_OF_RANGE_ERROR_CODE = "is.not.within.expected.range";
    private static final String DATE_IS_BEFORE_ERROR_CODE = "is.less.than.date";
    private static final String INVALID_MONTH_DAY_FORMAT_ERROR_CODE = "invalid.month.day.format";
    private static final String BEFORE_FIRST_EXECUTION_DATE_ERROR_CODE = "must.not.be.before.first.execution.date";
    private static final String MUST_BE_GREATER_THAN_ZERO_ERROR_CODE = "not.greater.than.zero";
    private static final String AMOUNT_NOT_ALLOWED_FOR_DUES_ERROR_CODE = "not.allowed.for.dues.instruction";
    private static final String CANNOT_TRANSFER_TO_SAME_ACCOUNT_ERROR_CODE = "transfer.to.same.account.not.allowed";
    private static final String INSTRUCTION_TYPE_DUES_NOT_ALLOWED_FOR_ACCOUNT_TRANSFER_ERROR_CODE = "dues.not.allowed.for.account.transfer";
    private static final String RECURRENCE_AS_PER_DUES_NOT_ALLOWED_FOR_SAVINGS_ERROR_CODE = "as.per.dues.not.allowed.for.account.transfer";
    private static final String ACCOUNT_TRANSFER_NOT_ALLOWED_FOR_LOAN_ERROR_CODE = "account.transfer.is.not.allowed.for.loan.accounts";
    private static final String RECURRENCE_AS_PER_DUES_NOT_ALLOWED_WITH_FIXED_INSTRUCTION_ERROR_CODE = "as.per.dues.not.allowed.with.fixed.amount";
    private static final String NOT_A_VALID_LOAN_REPAYMENT_ERROR_CODE = "is.not.a.valid.loan.repayment";
    
    
    private static final String invalidParamName = "invalidParam";
    private static final String invalidValue = "invalidValue";
    
    @Mock
    private AccountTransfersDetailDataValidator accountTransfersDetailDataValidator;
    
    private final static FromJsonHelper fromApiJsonHelper = new FromJsonHelper();
    private StandingInstructionDataValidator standingInstructionDataValidator;
    
    private boolean isUpdateMode = false;

    @BeforeEach
    public void setUp() {
        this.standingInstructionDataValidator = new StandingInstructionDataValidator(fromApiJsonHelper, 
            this.accountTransfersDetailDataValidator);
    }

    @Nested
    class WhenCreatingStandingInstruction {

        @Nested
        class BaseRules {
            @Test
            void shouldFailWhenRequestBodyIsNull(){
                assertThrowsException(InvalidJsonException.class, null);
            }

            @Test
            void shouldFailWhenRequestContainsUnknownParameter() {
                final JsonObject json = createAccountTransferRequest();
                json.addProperty(invalidParamName, invalidValue);

                assertThrowsException(UnsupportedParameterException.class, json);
            }

            @Test
            void shouldValidateAccountTransferDetails() {
                final JsonObject json = createAccountTransferRequest();
                standingInstructionDataValidator.validateForCreate(command(json));

                verify(accountTransfersDetailDataValidator, times(1))
                    .validate(
                        any(JsonCommand.class),
                        any(DataValidatorBuilder.class));
            }

            @ParameterizedTest
            @MethodSource("requiredBaseParameters")
            void shouldFailWhenRequiredParameterIsMissing(String parameter) {
                final JsonObject json = createAccountTransferRequest();
                json.remove(parameter);

                assertBlank(json, parameter);
            }

            @ParameterizedTest
            @MethodSource("parametersWithInvalidValues")
            void shouldFailWhenParameterHasInvalidValue(String parameter, Integer invalidValue) {
                final JsonObject json = createAccountTransferRequest();
                json.addProperty(parameter, invalidValue);

                assertRange(json, parameter);
            }

            @Test 
            void shouldFailWhenValidTillIsBeforeValidFrom() {
                final JsonObject json = createAccountTransferRequest();
                json.addProperty(StandingInstructionApiConstants.validTillParamName, "15 May 2026");

                assertValidation(json, 
                    StandingInstructionApiConstants.validTillParamName, DATE_IS_BEFORE_ERROR_CODE);
            }

            private static Stream<String> requiredBaseParameters() {
                return Stream.of(
                    AccountDetailConstants.transferTypeParamName,
                    StandingInstructionApiConstants.nameParamName,
                    StandingInstructionApiConstants.priorityParamName,
                    StandingInstructionApiConstants.instructionTypeParamName,
                    StandingInstructionApiConstants.statusParamName,
                    StandingInstructionApiConstants.validFromParamName,
                    StandingInstructionApiConstants.recurrenceTypeParamName
                );
            }
            
            private static Stream<Arguments> parametersWithInvalidValues() {
                return Stream.of(
                    Arguments.of(AccountDetailConstants.transferTypeParamName, 4),
                    Arguments.of(StandingInstructionApiConstants.priorityParamName, 5),
                    Arguments.of(StandingInstructionApiConstants.instructionTypeParamName, 3),
                    Arguments.of(StandingInstructionApiConstants.statusParamName, 3),
                    Arguments.of(StandingInstructionApiConstants.recurrenceTypeParamName, 3)
                );
            }
        }
        
        @Nested
        class PeriodicRecurrenceRules {
            @ParameterizedTest
            @MethodSource("requiredParameters")
            void shouldFailWhenPeriodicFieldIsMissing(String parameter) {
                final JsonObject json = createAccountTransferRequest();
                json.remove(parameter);

                assertBlank(json, parameter);
            }

            @Test
            void shouldFailWhenRecurrenceFrequencyHasInvalidValue() {
                final JsonObject json = createAccountTransferRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, 4);

                assertRange(json, StandingInstructionApiConstants.recurrenceFrequencyParamName);
            }

            @Test
            void shouldFailWhenRecurrenceOnMonthDayHasInvalidValue() {
                final JsonObject json = createAccountTransferRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceOnMonthDayParamName, "08 Mayo");

                assertValidation(json,
                    StandingInstructionApiConstants.recurrenceOnMonthDayParamName, INVALID_MONTH_DAY_FORMAT_ERROR_CODE);
            }

            @ParameterizedTest
            @MethodSource("invalidExecutionDates")
            void shouldFailWhenValidTillIsBeforeFirstExecution(String validTill, Integer recurrenceFrequency,
                Integer recurrenceInterval, String recurrenceOnMonthDay) {
                final JsonObject json = createAccountTransferRequest();
            
                json.addProperty(StandingInstructionApiConstants.validTillParamName, validTill);
                json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, recurrenceFrequency);                
                json.addProperty(StandingInstructionApiConstants.recurrenceIntervalParamName, recurrenceInterval);
                
                if (recurrenceOnMonthDay != null) {
                    json.addProperty(StandingInstructionApiConstants.recurrenceOnMonthDayParamName,
                        recurrenceOnMonthDay);
                }
            
                assertValidation(json, StandingInstructionApiConstants.validTillParamName, 
                    BEFORE_FIRST_EXECUTION_DATE_ERROR_CODE);
            }

            private static Stream<String> requiredParameters() {
                return Stream.of(
                    StandingInstructionApiConstants.recurrenceFrequencyParamName,
                    StandingInstructionApiConstants.recurrenceIntervalParamName,
                    StandingInstructionApiConstants.monthDayFormatParamName,
                    StandingInstructionApiConstants.recurrenceOnMonthDayParamName
                );
            }

            private static Stream<Arguments> invalidExecutionDates() {
                return Stream.of(
                    Arguments.of("20 May 2026", 0, 5, null),
                    Arguments.of("29 May 2026", 1, 2, null),
                    Arguments.of("17 May 2026", 2, 1, "18 May")
                );
            }

        }

        @Nested
        class AmountRules {
            @Test
            void shouldFailWhenAmountIsMissing() {
                final JsonObject json = createAccountTransferRequest();
                json.remove(StandingInstructionApiConstants.amountParamName);

                assertBlank(json, StandingInstructionApiConstants.amountParamName);
            }

            @Test
            void shouldFailWhenAmountValueIsNotPositive() {
                final JsonObject json = createAccountTransferRequest();
                json.addProperty(StandingInstructionApiConstants.amountParamName, BigDecimal.valueOf(-10.00));
                
                assertValidation(json,
                    StandingInstructionApiConstants.amountParamName, MUST_BE_GREATER_THAN_ZERO_ERROR_CODE);
            }
        }

        @Nested
        class AccountTransferRules {
            @Test
            void shouldFailWithEqualAccountsAndEqualOffices() {
                final JsonObject json = createAccountTransferRequest();
                json.addProperty(AccountDetailConstants.toAccountIdParamName, 1);
                
                assertValidation(json, 
                    AccountDetailConstants.toAccountIdParamName, CANNOT_TRANSFER_TO_SAME_ACCOUNT_ERROR_CODE);
            }

            @Test
            void shouldFailWhenInstructionTypeIsDues() {
                final JsonObject json = createAccountTransferRequest();
                json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 2);

                assertValidation(json,
                    StandingInstructionApiConstants.instructionTypeParamName,
                    INSTRUCTION_TYPE_DUES_NOT_ALLOWED_FOR_ACCOUNT_TRANSFER_ERROR_CODE);
            }

            @Test
            void shouldFailWhenRecurrenceTypeIsAsPerDues() {
                final JsonObject json = createAccountTransferRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 2);
                
                assertValidation(json,
                    StandingInstructionApiConstants.recurrenceTypeParamName,
                    RECURRENCE_AS_PER_DUES_NOT_ALLOWED_FOR_SAVINGS_ERROR_CODE);
            }

            @Test
            void shouldFailWhenAccountTransferInvolvesLoanAccount() {
                final JsonObject json = createAccountTransferRequest();
                json.addProperty(AccountDetailConstants.fromAccountTypeParamName, 1);

                assertValidation(json,
                    AccountDetailConstants.transferTypeParamName,
                    ACCOUNT_TRANSFER_NOT_ALLOWED_FOR_LOAN_ERROR_CODE);
            }

            @Test
            void shouldPassWithDailyPeriodicRecurrence() {
                final JsonObject json = createAccountTransferRequest();
                
                json.remove(StandingInstructionApiConstants.recurrenceOnMonthDayParamName);                
                json.remove(StandingInstructionApiConstants.monthDayFormatParamName);
                
                json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, 0);

                assertValidationSuccess(json);
            }

            @Test
            void shouldPassWithYearlyPeriodicRecurrence() {
                final JsonObject json = createAccountTransferRequest(); 
                json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, 3);

                assertValidationSuccess(json);
            }
        }

        @Nested
        class LoanRepaymentRules {
            @Test
            void shouldFailWhenInstructionTypeIsFixedAndRecurrenceTypeIsAsPerDues() {
                final JsonObject json = createLoanRepaymentRequest();
                json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 1);

                assertValidation(json,
                    StandingInstructionApiConstants.recurrenceTypeParamName,
                    RECURRENCE_AS_PER_DUES_NOT_ALLOWED_WITH_FIXED_INSTRUCTION_ERROR_CODE);
            }
            
            @Test
            void shouldFailWhenInstructionTypeIsDuesAndAmountIsNotNull() {
                final JsonObject json = createLoanRepaymentRequest();
                json.addProperty(StandingInstructionApiConstants.amountParamName, BigDecimal.TEN);

                assertValidation(json,
                    StandingInstructionApiConstants.amountParamName, AMOUNT_NOT_ALLOWED_FOR_DUES_ERROR_CODE);
            }

            @Test
            void shouldFailWhenIsNotAValidLoanRepayment() {
                final JsonObject json = createLoanRepaymentRequest();
                json.addProperty(AccountDetailConstants.toAccountTypeParamName, 2);

                assertValidation(json,
                    AccountDetailConstants.transferTypeParamName, NOT_A_VALID_LOAN_REPAYMENT_ERROR_CODE);
            }

            @Test
            void shouldPassWithFixedAmountAndPeriodicRecurrence() {
                final JsonObject json = createLoanRepaymentRequest();
                json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 1);
                json.addProperty(StandingInstructionApiConstants.amountParamName, BigDecimal.TEN);
                json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 1);
                json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, 2);
                json.addProperty(StandingInstructionApiConstants.recurrenceIntervalParamName, 1);
                json.addProperty(StandingInstructionApiConstants.recurrenceOnMonthDayParamName, "15 May");
                json.addProperty(StandingInstructionApiConstants.monthDayFormatParamName, "dd MMMM");
                
                assertValidationSuccess(json);
            }

            @Test
            void shouldPassWithPeriodicRecurrence() {
                final JsonObject json = createLoanRepaymentRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 1);
                json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, 1);
                json.addProperty(StandingInstructionApiConstants.recurrenceIntervalParamName, 20);
                
                assertValidationSuccess(json);
            }

            @Test
            void shouldPassWithTraditionalData() {
                assertValidationSuccess(createLoanRepaymentRequest());
            }
        }
    }

    @Nested
    class WhenUpdatingStandingInstruction {
        @BeforeEach
        public void setUpUpdateMode() {
            isUpdateMode = true;
        }

        @Test
        void throwExceptionWhenJsonIsBlankOrNull() {
            assertThrowsException(InvalidJsonException.class, null);
        }
    }

    private JsonCommand command(JsonObject json) {
        final String j = json == null ? "" : jsonObject.toString();
        final JsonElement element = fromApiJsonHelper.parse(json);

        return JsonCommand.from(j, element, fromApiJsonHelper, null,
            null, null, null, null, null, null, null, null, null, null, null,
            null, null);
    }

    private JsonObject commonValuesInCreateRequest(){
        JsonObject json = new JsonObject();
        json.addProperty(AccountDetailConstants.localeParamName, "en");
        json.addProperty(AccountDetailConstants.dateFormatParamName, "dd MMMM yyyy");
        json.addProperty(AccountDetailConstants.fromOfficeIdParamName, 1);
        json.addProperty(AccountDetailConstants.fromClientIdParamName, 1);
        json.addProperty(AccountDetailConstants.fromAccountIdParamName, 1);
        json.addProperty(AccountDetailConstants.fromAccountTypeParamName, 2);
        json.addProperty(AccountDetailConstants.toOfficeIdParamName, 1);
        json.addProperty(AccountDetailConstants.toClientIdParamName, 1);
        json.addProperty(StandingInstructionApiConstants.priorityParamName, 1);
        json.addProperty(StandingInstructionApiConstants.statusParamName, 1);
        json.addProperty(StandingInstructionApiConstants.validFromParamName, "16 May 2026");
        json.addProperty(StandingInstructionApiConstants.validTillParamName, "16 May 2027");

        return json;
    }

    private JsonObject createAccountTransferRequest() {
        JsonObject json = commonValuesInCreateRequest();
        json.addProperty(AccountDetailConstants.toAccountIdParamName, 2);
        json.addProperty(AccountDetailConstants.toAccountTypeParamName, 2);
        json.addProperty(AccountDetailConstants.transferTypeParamName, 1);
        json.addProperty(StandingInstructionApiConstants.nameParamName, "BASIC ACCOUNT TRANSFER");
        json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 1);
        json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 1);
        json.addProperty(StandingInstructionApiConstants.amountParamName, BigDecimal.TEN);
        json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, 2);
        json.addProperty(StandingInstructionApiConstants.recurrenceIntervalParamName, 1);
        json.addProperty(StandingInstructionApiConstants.recurrenceOnMonthDayParamName, "15 May");
        json.addProperty(StandingInstructionApiConstants.monthDayFormatParamName, "dd MMMM");

        return json;
    }

    private JsonObject createLoanRepaymentRequest() {
        JsonObject json = commonValuesInCreateRequest();
        json.addProperty(AccountDetailConstants.toAccountIdParamName, 1);
        json.addProperty(AccountDetailConstants.toAccountTypeParamName, 1);
        json.addProperty(AccountDetailConstants.transferTypeParamName, 2);
        json.addProperty(StandingInstructionApiConstants.nameParamName, "BASIC LOAN REPAYMENT");
        json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 2);
        json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 2);

        return json;
    }

    private void validate(final JsonObject json) {
        if(isUpdateMode) {
            this.standingInstructionDataValidator.validateForUpdate(command(json));
        } else {
            this.standingInstructionDataValidator.validateForCreate(command(json));
        }
    }

    private void assertThrowsException(Class<? extends Throwable> exceptionClass, JsonObject json) {
        assertThrows(exceptionClass, () -> validate(json));
    }

    private void assertValidation(final JsonObject json, final String parameter, final String reason) {
        final String expectedCode = STANDING_INSTRUCTION_RESOURCE_NAME_PREFIX + parameter + "." + reason;
        
        PlatformApiDataValidationException ex = assertThrows(PlatformApiDataValidationException.class, () -> validate(json));

        boolean hasError = ex.getErrors().stream().anyMatch(error -> 
            parameter.equals(error.getParameterName()) &&
            expectedCode.equals(error.getUserMessageGlobalisationCode()));

        assertTrue(hasError);
    }

    private void assertBlank(final JsonObject json, final String parameter) {
        assertValidation(json, parameter, CANNOT_BE_BLANK_ERROR_CODE);
    }

    private void assertRange(final JsonObject json, final String parameter) {
        assertValidation(json, parameter, OUT_OF_RANGE_ERROR_CODE);
    }

    private void assertValidationSuccess(JsonObject json) {
        assertDoesNotThrow(() -> validate(json));
    }
}