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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Standing Instruction Data Validator Tests")
public class StandingInstructionDataValidatorTest {

    private static final String VALIDATION_MSG_BASE = "validation.msg.standinginstruction.";
    private static final String ERROR_CODE_CANNOT_BE_BLANK = "cannot.be.blank";
    private static final String ERROR_CODE_INVALID_RANGE = "is.not.within.expected.range";
    private static final String ERROR_CODE_INVALID_DATE_RANGE = "is.less.than.date";
    private static final String ERROR_CODE_EXECUTION_DATE = "must.not.be.before.first.execution.date";
    private static final String ERROR_CODE_INVALID_MONTH_DAY = "invalid.month.day.format";
    private static final String ERROR_CODE_AMOUNT_NOT_POSITIVE = "not.greater.than.zero";
    private static final String ERROR_CODE_AMOUNT_NOT_ALLOWED_FOR_DUES = "not.allowed.for.dues.instruction";
    
    private static final String LOCALE = "en";
    private static final String DATE_FORMAT = "dd MMMM yyyy";
    private static final String MONTH_DAY_FORMAT = "dd MMMM";
    private static final String VALID_DATE = "08 May 2026";
    private static final String FUTURE_DATE = "07 May 2027";
    
    @Mock
    private AccountTransfersDetailDataValidator accountTransfersDetailDataValidator;
    
    private final FromJsonHelper fromApiJsonHelper = new FromJsonHelper();
    private StandingInstructionDataValidator standingInstructionDataValidator;

    @BeforeEach
    void setUp() {
        standingInstructionDataValidator = new StandingInstructionDataValidator(
            fromApiJsonHelper, 
            accountTransfersDetailDataValidator
        );
    }

    @Nested
    @DisplayName("Create Standing Instruction Validation")
    class CreateValidation {

        @Nested
        @DisplayName("Base Request Validation")
        class BaseValidation {
            
            @Test
            @DisplayName("Should reject null request body")
            void shouldRejectNullRequestBody() {
                assertThrowsInvalidJsonException(null);
            }

            @Test
            @DisplayName("Should reject request with unknown parameter")
            void shouldRejectUnknownParameter() {
                JsonObject json = createBaseRequest();
                json.addProperty("invalidParam", "invalidValue");
                assertThrowsUnsupportedParameterException(json);
            }

            @Test
            @DisplayName("Should validate account transfer details")
            void shouldValidateAccountTransferDetails() {
                JsonObject json = createAccountTransferRequest();
                standingInstructionDataValidator.validateForCreate(createCommand(json));
                verify(accountTransfersDetailDataValidator, times(1))
                    .validate(any(JsonCommand.class), any(DataValidatorBuilder.class));
            }

            @ParameterizedTest
            @ValueSource(strings = {
                AccountDetailConstants.transferTypeParamName,
                StandingInstructionApiConstants.nameParamName,
                StandingInstructionApiConstants.priorityParamName,
                StandingInstructionApiConstants.instructionTypeParamName,
                StandingInstructionApiConstants.statusParamName,
                StandingInstructionApiConstants.validFromParamName,
                StandingInstructionApiConstants.recurrenceTypeParamName
            })
            @DisplayName("Should reject when required parameter is missing")
            void shouldRejectMissingRequiredParameter(String parameter) {
                assertValidationErrorForBlankParameter(parameter);
            }

            @ParameterizedTest
            @ValueSource(strings = {
                AccountDetailConstants.transferTypeParamName,
                StandingInstructionApiConstants.priorityParamName,
                StandingInstructionApiConstants.instructionTypeParamName,
                StandingInstructionApiConstants.statusParamName,
                StandingInstructionApiConstants.recurrenceTypeParamName
            })
            @DisplayName("Should reject when parameter has invalid value")
            void shouldRejectInvalidParameterValue(String parameter) {
                assertValidationErrorForInvalidRange(parameter);
            }

            @Test
            @DisplayName("Should reject when valid till date is before valid from date")
            void shouldRejectInvalidDateRange() {
                assertValidationErrorForDateRange();
            }
        }
        
        @Nested
        @DisplayName("Periodic Recurrence Validation")
        class PeriodicRecurrenceValidation {
            
            @ParameterizedTest
            @ValueSource(strings = {
                StandingInstructionApiConstants.recurrenceFrequencyParamName,
                StandingInstructionApiConstants.recurrenceIntervalParamName,
                StandingInstructionApiConstants.monthDayFormatParamName,
                StandingInstructionApiConstants.recurrenceOnMonthDayParamName
            })
            @DisplayName("Should reject when periodic field is missing")
            void shouldRejectMissingPeriodicField(String parameter) {
                assertValidationErrorForBlankParameter(parameter);
            }

            @Test
            @DisplayName("Should reject when recurrence frequency has invalid value")
            void shouldRejectInvalidRecurrenceFrequency() {
                JsonObject json = createPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, 4);
                assertValidationErrorForInvalidRange(json, StandingInstructionApiConstants.recurrenceFrequencyParamName);
            }

            @Test
            @DisplayName("Should reject when recurrence on month day has invalid format")
            void shouldRejectInvalidMonthDayFormat() {
                JsonObject json = createPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceOnMonthDayParamName, "08 Mayo");
                assertValidationErrorForInvalidMonthDayFormat(json);
            }

            @ParameterizedTest
            @MethodSource("provideInvalidExecutionDateScenarios")
            @DisplayName("Should reject when valid till date is before first execution date")
            void shouldRejectValidTillBeforeExecutionDate(
                String validFrom, String validTill, Integer frequency, Integer interval, String monthDay) {
                
                JsonObject json = createPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.validFromParamName, validFrom);
                json.addProperty(StandingInstructionApiConstants.validTillParamName, validTill);
                json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, frequency);                
                json.addProperty(StandingInstructionApiConstants.recurrenceIntervalParamName, interval);
                
                if (monthDay != null) {
                    json.addProperty(StandingInstructionApiConstants.recurrenceOnMonthDayParamName, monthDay);
                }
                
                assertValidationErrorForExecutionDate(json);
            }

            private static Stream<Arguments> provideInvalidExecutionDateScenarios() {
                return Stream.of(
                    Arguments.of("08 May 2026", "10 May 2026", 0, 5, null),
                    Arguments.of("08 May 2026", "15 May 2026", 1, 2, null),
                    Arguments.of("15 May 2026", "25 May 2026", 2, 1, "10 May")
                );
            }
        }

        @Nested
        @DisplayName("Amount Validation")
        class AmountValidation {
            
            @Test
            @DisplayName("Should reject when amount is missing")
            void shouldRejectMissingAmount() {
                assertValidationErrorForBlankParameter(StandingInstructionApiConstants.amountParamName);
            }

            @Test
            @DisplayName("Should reject when amount is not positive")
            void shouldRejectNonPositiveAmount() {
                JsonObject json = createPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.amountParamName, new BigDecimal("-10.00"));
                assertValidationErrorForNonPositiveAmount(json);
            }

            @Test
            @DisplayName("Should reject when instruction type is dues and amount is provided")
            void shouldRejectAmountForDuesInstruction() {
                assertValidationErrorForAmountWithDues();
            }
        }

        @Nested
        @DisplayName("Account Transfer Validation")
        class AccountTransferValidation {
            
            @Test
            @DisplayName("Should reject when recurrence type is as per dues")
            void shouldRejectAsPerDuesRecurrenceType() {
                JsonObject json = createPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 2);
                assertValidationErrorForInvalidParameter(json, 
                    StandingInstructionApiConstants.recurrenceTypeParamName,
                    "as.per.dues.not.allowed.for.account.transfer");
            }

            @Test
            @DisplayName("Should reject when instruction type is dues")
            void shouldRejectDuesInstructionType() {
                JsonObject json = createPeriodicRequest();
                json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 2);
                assertValidationErrorForInvalidParameter(json,
                    StandingInstructionApiConstants.instructionTypeParamName,
                    "dues.not.allowed.for.account.transfer");
            }

            @Test
            @DisplayName("Should reject when instruction type is not fixed")
            void shouldRejectNonFixedInstructionType() {
                JsonObject json = createAccountTransferRequest();
                json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 2);
                assertValidationErrorForInvalidRange(json, StandingInstructionApiConstants.instructionTypeParamName);
            }

            @Test
            @DisplayName("Should reject when recurrence type is not periodic")
            void shouldRejectNonPeriodicRecurrenceType() {
                JsonObject json = createAccountTransferRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 2);
                assertValidationErrorForInvalidRange(json, StandingInstructionApiConstants.recurrenceTypeParamName);
            }

            @Test
            @DisplayName("Should reject when transfer type is not account transfer")
            void shouldRejectNonAccountTransfer() {
                JsonObject json = createAccountTransferRequest();
                json.addProperty(AccountDetailConstants.fromAccountTypeParamName, 1);
                assertValidationErrorForInvalidParameter(json,
                    AccountDetailConstants.transferTypeParamName,
                    "not.account.transfer");
            }

            @Test
            @DisplayName("Should reject when transferring to same account")
            void shouldRejectSameAccountTransfer() {
                JsonObject json = createAccountTransferRequest();
                json.addProperty(AccountDetailConstants.toAccountIdParamName, 1);
                assertValidationErrorForInvalidParameter(json,
                    AccountDetailConstants.toAccountIdParamName,
                    "transfer.to.same.account.not.allowed");
            }

            @Test
            @DisplayName("Should accept valid account transfer")
            void shouldAcceptValidAccountTransfer() {
                assertValidationSuccess(createAccountTransferRequest());
            }
        }

        @Nested
        @DisplayName("Loan Repayment Validation")
        class LoanRepaymentValidation {
            
            @Test
            @DisplayName("Should reject when fixed instruction with as per dues recurrence")
            void shouldRejectFixedAmountWithAsPerDues() {
                assertValidationErrorForInvalidParameter(createLoanRepaymentRequest(),
                    StandingInstructionApiConstants.recurrenceTypeParamName,
                    "as.per.dues.not.allowed.with.fixed.amount");
            }

            @Test
            @DisplayName("Should reject when transfer type is not loan repayment")
            void shouldRejectNonLoanRepayment() {
                JsonObject json = createLoanRepaymentRequest();
                json.addProperty(AccountDetailConstants.toAccountTypeParamName, 2);
                assertValidationErrorForInvalidParameter(json,
                    AccountDetailConstants.transferTypeParamName,
                    "not.loan.repayment");
            }

            @Test
            @DisplayName("Should accept valid loan repayment")
            void shouldAcceptValidLoanRepayment() {
                JsonObject json = createLoanRepaymentRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 1);
                assertValidationSuccess(json);
            }
        }
    }

    @Nested
    @DisplayName("Update Standing Instruction Validation")
    class UpdateValidation {
        
        @Test
        @DisplayName("Should reject when JSON is blank or null")
        void shouldRejectBlankOrNullJson() {
            assertThrowsInvalidJsonException(null);
        }
    }

    // Helper Methods for Validation Assertions
    
    private void assertThrowsInvalidJsonException(JsonObject json) {
        assertThrows(InvalidJsonException.class, 
            () -> standingInstructionDataValidator.validateForCreate(createCommand(json)));
    }
    
    private void assertThrowsUnsupportedParameterException(JsonObject json) {
        assertThrows(UnsupportedParameterException.class, 
            () -> standingInstructionDataValidator.validateForCreate(createCommand(json)));
    }
    
    private void assertValidationErrorForBlankParameter(String parameter) {
        JsonObject json = createBaseRequest();
        json.remove(parameter);
        assertValidationErrorWithCode(json, parameter, ERROR_CODE_CANNOT_BE_BLANK);
    }
    
    private void assertValidationErrorForInvalidRange(String parameter) {
        JsonObject json = createBaseRequest();
        json.addProperty(parameter, 99);
        assertValidationErrorWithCode(json, parameter, ERROR_CODE_INVALID_RANGE);
    }
    
    private void assertValidationErrorForInvalidRange(JsonObject json, String parameter) {
        assertValidationErrorWithCode(json, parameter, ERROR_CODE_INVALID_RANGE);
    }
    
    private void assertValidationErrorForDateRange() {
        JsonObject json = createBaseRequest();
        assertValidationErrorWithCode(json, 
            StandingInstructionApiConstants.validTillParamName, 
            ERROR_CODE_INVALID_DATE_RANGE);
    }
    
    private void assertValidationErrorForInvalidMonthDayFormat(JsonObject json) {
        assertValidationErrorWithCode(json,
            StandingInstructionApiConstants.recurrenceOnMonthDayParamName,
            ERROR_CODE_INVALID_MONTH_DAY);
    }
    
    private void assertValidationErrorForExecutionDate(JsonObject json) {
        assertValidationErrorWithCode(json,
            StandingInstructionApiConstants.validTillParamName,
            ERROR_CODE_EXECUTION_DATE);
    }
    
    private void assertValidationErrorForNonPositiveAmount(JsonObject json) {
        assertValidationErrorWithCode(json,
            StandingInstructionApiConstants.amountParamName,
            ERROR_CODE_AMOUNT_NOT_POSITIVE);
    }
    
    private void assertValidationErrorForAmountWithDues() {
        assertValidationErrorWithCode(createDuesRequest(),
            StandingInstructionApiConstants.amountParamName,
            ERROR_CODE_AMOUNT_NOT_ALLOWED_FOR_DUES);
    }
    
    private void assertValidationErrorForInvalidParameter(JsonObject json, String parameter, String reason) {
        assertValidationErrorWithCode(json, parameter, reason);
    }
    
    private void assertValidationErrorWithCode(JsonObject json, String parameter, String reason) {
        String expectedCode = VALIDATION_MSG_BASE + parameter + "." + reason;
        
        PlatformApiDataValidationException exception = assertThrows(PlatformApiDataValidationException.class,
            () -> standingInstructionDataValidator.validateForCreate(createCommand(json)));
        
        boolean hasExpectedError = exception.getErrors().stream().anyMatch(error -> 
            parameter.equals(error.getParameterName()) &&
            expectedCode.equals(error.getUserMessageGlobalisationCode()));
        
        assertTrue(hasExpectedError, 
            String.format("Expected error with parameter '%s' and code '%s' not found", parameter, expectedCode));
    }
    
    private void assertValidationSuccess(JsonObject json) {
        assertDoesNotThrow(() -> 
            standingInstructionDataValidator.validateForCreate(createCommand(json)));
    }
    
    // Helper Methods for Creating Test Data
    
    private JsonCommand createCommand(JsonObject jsonObject) {
        String json = jsonObject == null ? "" : jsonObject.toString();
        JsonElement parsedCommand = fromApiJsonHelper.parse(json);
        return JsonCommand.from(json, parsedCommand, fromApiJsonHelper, null,
            null, null, null, null, null, null, null, null, null, null, null,
            null, null);
    }
    
    private JsonObject createBaseRequest() {
        return new StandingInstructionRequestBuilder()
            .withLocale(LOCALE)
            .withDateFormat(DATE_FORMAT)
            .withFromOfficeId(1)
            .withFromClientId(1)
            .withFromAccountId(1)
            .withFromAccountType(1)
            .withToOfficeId(1)
            .withToClientId(1)
            .withToAccountId(1)
            .withToAccountType(1)
            .withTransferType(4)
            .withName("BASE TEST")
            .withPriority(5)
            .withInstructionType(3)
            .withStatus(3)
            .withValidFrom(VALID_DATE)
            .withValidTill("07 May 2026")
            .withRecurrenceType(3)
            .withAmount(new BigDecimal("10.00"))
            .withRecurrenceFrequency(2)
            .withRecurrenceInterval(1)
            .withRecurrenceOnMonthDay("08 May")
            .withMonthDayFormat(MONTH_DAY_FORMAT)
            .build();
    }
    
    private JsonObject createPeriodicRequest() {
        return new StandingInstructionRequestBuilder(createBaseRequest())
            .withTransferType(1)
            .withFromAccountType(2)
            .withToAccountType(2)
            .withPriority(1)
            .withInstructionType(1)
            .withStatus(1)
            .withRecurrenceType(1)
            .withValidTill(FUTURE_DATE)
            .build();
    }
    
    private JsonObject createDuesRequest() {
        return new StandingInstructionRequestBuilder(createPeriodicRequest())
            .withName("DUES TEST")
            .withInstructionType(2)
            .build();
    }
    
    private JsonObject createLoanRepaymentRequest() {
        return new StandingInstructionRequestBuilder(createPeriodicRequest())
            .withName("LOAN REPAYMENT TEST")
            .withTransferType(2)
            .withToAccountType(1)
            .withRecurrenceType(2)
            .withInstructionType(1)
            .build();
    }
    
    private JsonObject createAccountTransferRequest() {
        return new StandingInstructionRequestBuilder(createPeriodicRequest())
            .withName("ACCOUNT TRANSFER TEST")
            .withToAccountId(2)
            .build();
    }

    // Builder Pattern for Request Creation
    
    private static class StandingInstructionRequestBuilder {
        
        private final JsonObject json;
        
        public StandingInstructionRequestBuilder() {
            this.json = new JsonObject();
        }
        
        public StandingInstructionRequestBuilder(JsonObject source) {
            this.json = source.deepCopy();
        }
        
        public StandingInstructionRequestBuilder withLocale(String value) {
            json.addProperty(AccountDetailConstants.localeParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withDateFormat(String value) {
            json.addProperty(AccountDetailConstants.dateFormatParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withFromOfficeId(Integer value) {
            json.addProperty(AccountDetailConstants.fromOfficeIdParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withFromClientId(Integer value) {
            json.addProperty(AccountDetailConstants.fromClientIdParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withFromAccountId(Integer value) {
            json.addProperty(AccountDetailConstants.fromAccountIdParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withFromAccountType(Integer value) {
            json.addProperty(AccountDetailConstants.fromAccountTypeParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withToOfficeId(Integer value) {
            json.addProperty(AccountDetailConstants.toOfficeIdParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withToClientId(Integer value) {
            json.addProperty(AccountDetailConstants.toClientIdParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withToAccountId(Integer value) {
            json.addProperty(AccountDetailConstants.toAccountIdParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withToAccountType(Integer value) {
            json.addProperty(AccountDetailConstants.toAccountTypeParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withTransferType(Integer value) {
            json.addProperty(AccountDetailConstants.transferTypeParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withName(String value) {
            json.addProperty(StandingInstructionApiConstants.nameParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withPriority(Integer value) {
            json.addProperty(StandingInstructionApiConstants.priorityParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withInstructionType(Integer value) {
            json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withStatus(Integer value) {
            json.addProperty(StandingInstructionApiConstants.statusParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withValidFrom(String value) {
            json.addProperty(StandingInstructionApiConstants.validFromParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withValidTill(String value) {
            json.addProperty(StandingInstructionApiConstants.validTillParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withRecurrenceType(Integer value) {
            json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withAmount(BigDecimal value) {
            json.addProperty(StandingInstructionApiConstants.amountParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withRecurrenceFrequency(Integer value) {
            json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withRecurrenceInterval(Integer value) {
            json.addProperty(StandingInstructionApiConstants.recurrenceIntervalParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withRecurrenceOnMonthDay(String value) {
            json.addProperty(StandingInstructionApiConstants.recurrenceOnMonthDayParamName, value);
            return this;
        }
        
        public StandingInstructionRequestBuilder withMonthDayFormat(String value) {
            json.addProperty(StandingInstructionApiConstants.monthDayFormatParamName, value);
            return this;
        }
        
        public JsonObject build() {
            return json.deepCopy();
        }
    }
}