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
    private static final String STANDING_INSTRUCTION_MSG_BASE = "validation.msg.standinginstruction.";
    private static final String MSG_CODE_CANNOT_BE_BLANK = "cannot.be.blank";
    private static final String MSG_CODE_INVALID_RANGE = "is.not.within.expected.range";
    
    @Mock
    private AccountTransfersDetailDataValidator accountTransfersDetailDataValidator;
    
    private final static FromJsonHelper fromApiJsonHelper = new FromJsonHelper();
    private StandingInstructionDataValidator standingInstructionDataValidator;

    @BeforeEach
    public void setUp() {
        this.standingInstructionDataValidator = new StandingInstructionDataValidator(fromApiJsonHelper, 
            this.accountTransfersDetailDataValidator);
    }

    @Nested
    class WhenCreatingStandingInstruction {

        @Nested
        class BaseValidation {
            @Test
            void shouldFailWhenRequestBodyIsNull(){
                assertThrowsException(InvalidJsonException.class, null);
            }

            @Test
            void shouldFailWhenRequestContainsUnknownParameter() {
                final JsonObject json = baseRequest();
                json.addProperty("invalidParam", "invalidValue");
                assertThrowsException(UnsupportedParameterException.class, json);
            }

            @Test
            void shouldValidateAccountTransferDetails() {
                final JsonObject json = accountTransferRequest();
                standingInstructionDataValidator.validateForCreate(command(json));

                verify(accountTransfersDetailDataValidator, times(1))
                    .validate(
                        any(JsonCommand.class),
                        any(DataValidatorBuilder.class));
            }

            @ParameterizedTest
            @ValueSource(strings = {
                AccountDetailConstants.transferTypeParamName,
                StandingInstructionApiConstants.nameParamName,
                StandingInstructionApiConstants.priorityParamName,
                StandingInstructionApiConstants.instructionTypeParamName,
                StandingInstructionApiConstants.statusParamName,
                StandingInstructionApiConstants.validFromParamName,
                StandingInstructionApiConstants.recurrenceTypeParamName })
            void shouldFailWhenRequiredParameterIsMissing(String parameter) {
                assertBlank(baseRequest(), parameter);
            }

            @ParameterizedTest
            @ValueSource(strings = {
                AccountDetailConstants.transferTypeParamName,
                StandingInstructionApiConstants.priorityParamName,
                StandingInstructionApiConstants.instructionTypeParamName,
                StandingInstructionApiConstants.statusParamName,
                StandingInstructionApiConstants.recurrenceTypeParamName
            })
            void shouldFailWhenParameterHasInvalidValue(String parameter) {
                assertRange(baseRequest(), parameter);
            }

            @Test 
            void shouldFailWhenValidTillDateIsBeforeValidFromDate() {
                assertValidation(baseRequest(), 
                    StandingInstructionApiConstants.validTillParamName, "is.less.than.date");
            }
        }
        
        @Nested
        class PeriodicRecurrenceType {
            @ParameterizedTest
            @ValueSource(strings = {
                StandingInstructionApiConstants.recurrenceFrequencyParamName,
                StandingInstructionApiConstants.recurrenceIntervalParamName,
                StandingInstructionApiConstants.monthDayFormatParamName,
                StandingInstructionApiConstants.recurrenceOnMonthDayParamName
            })
            void shouldFailWhenPeriodicFieldIsMissing(String parameter) {
                assertBlank(periodicRequest(), parameter);
            }

            @Test
            void shouldFailWhenRecurrenceFrequencyHasInvalidValue() {
                final JsonObject json = periodicRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, 4);
                assertRange(json, StandingInstructionApiConstants.recurrenceFrequencyParamName);
            }

            @Test
            void shouldFailWhenRecurrenceOnMonthDayHasInvalidValue() {
                final JsonObject json = periodicRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceOnMonthDayParamName, "08 Mayo");
                assertValidation(json,
                    StandingInstructionApiConstants.recurrenceOnMonthDayParamName, "invalid.month.day.format");
            }

            @ParameterizedTest
            @MethodSource("invalidExecutionDates")
            void shouldFailWhenValidTillDateIsBeforeFirstExecutionDate(String validFrom, String validTill,
                Integer recurrenceFrequency, Integer recurrenceInterval, String recurrenceOnMonthDay) {
                
                final JsonObject json = periodicRequest();
                
                json.addProperty(StandingInstructionApiConstants.validFromParamName, validFrom);
                json.addProperty(StandingInstructionApiConstants.validTillParamName, validTill);
                json.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, recurrenceFrequency);                
                json.addProperty(StandingInstructionApiConstants.recurrenceIntervalParamName, recurrenceInterval);
                
                if (recurrenceOnMonthDay != null) {
                    json.addProperty(StandingInstructionApiConstants.recurrenceOnMonthDayParamName, recurrenceOnMonthDay);
                }
            
                assertValidation(json,
                    StandingInstructionApiConstants.validTillParamName,
                    "must.not.be.before.first.execution.date");
            }

            private static Stream<Arguments> invalidExecutionDates() {
                return Stream.of(
                    Arguments.of("08 May 2026", "10 May 2026", 0, 5, null),
                    Arguments.of("08 May 2026", "15 May 2026", 1, 2, null),
                    Arguments.of("15 May 2026", "25 May 2026", 2, 1, "10 May")
                );
            }

        }

        @Nested
        class Amount {
            @Test
            void shouldFailWhenAmountIsMissing() {
                final JsonObject json = periodicRequest();
                assertBlank(json, 
                    StandingInstructionApiConstants.amountParamName);
            }

            @Test
            void shouldFailWhenAmountValueIsNotPositive() {
                final JsonObject json = periodicRequest();
                json.addProperty(StandingInstructionApiConstants.amountParamName, new BigDecimal("-10.00"));
                assertValidation(json,
                    StandingInstructionApiConstants.amountParamName, "not.greater.than.zero");
            }

            @Test
            void shouldFailWhenInstructionTypeIsDuesAndAmountIsNotNull() {
                assertValidation(duesRequest(),
                    StandingInstructionApiConstants.amountParamName, "not.allowed.for.dues.instruction");
            }
        }

        @Nested
        class AccountTransfer {
            @Test
            void shouldFailWhenRecurrenceTypeIsAsPerDues() {
                final JsonObject json = periodicRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 2);
                assertValidation(json,
                    StandingInstructionApiConstants.recurrenceTypeParamName,
                    "as.per.dues.not.allowed.for.account.transfer");
            }

            @Test
            void shouldFailWhenInstructionTypeIsDues() {
                final JsonObject json = periodicRequest();
                json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 2);
                assertValidation(json,
                    StandingInstructionApiConstants.instructionTypeParamName, "dues.not.allowed.for.account.transfer");
            }

            @Test
            void shouldFailWhenInstructionTypeIsNotFixed() {
                final JsonObject json = accountTransferRequest();
                json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 2);
                assertRange(json,
                    StandingInstructionApiConstants.instructionTypeParamName);
            }

            @Test
            void shouldFailWhenRecurrenceTypeIsNotPeriodic() {
                final JsonObject json = accountTransferRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 2);
                assertRange(json,
                    StandingInstructionApiConstants.recurrenceTypeParamName);
            }

            @Test
            void shouldFailWhenIsNotAnAccountTransfer() {
                final JsonObject json = accountTransferRequest();
                json.addProperty(AccountDetailConstants.fromAccountTypeParamName, 1);
                assertValidation(json,
                    AccountDetailConstants.transferTypeParamName, "not.account.transfer");
            }

            @Test
            void shouldFailWithEqualAccountsAndEqualOffices() {
                final JsonObject json = accountTransferRequest();
                json.addProperty(AccountDetailConstants.toAccountIdParamName, 1);
                assertValidation(json, 
                    AccountDetailConstants.toAccountIdParamName, "transfer.to.same.account.not.allowed");
            }

            @Test
            void shouldPassWithValidAccountTransfer() {
                assertValidationSuccess(accountTransferRequest());
            }
        }

        @Nested
        class LoanRepayment {
            @Test
            void shouldFailWhenInstructionTypeIsFixedAndRecurrenceTypeIsAsPerDues() {
                assertValidation(loanRepaymentRequest(),
                    StandingInstructionApiConstants.recurrenceTypeParamName,
                    "as.per.dues.not.allowed.with.fixed.amount");
            }

            @Test
            void shouldFailWhenIsNotALoanRepayment() {
                final JsonObject json = loanRepaymentRequest();
                json.addProperty(AccountDetailConstants.toAccountTypeParamName, 2);
                assertValidation(json,
                    AccountDetailConstants.transferTypeParamName, "not.loan.repayment");
            }

            @Test
            void shouldPassWithValidLoanRepayment() {
                final JsonObject json = loanRepaymentRequest();
                json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 1);
                
                assertValidationSuccess(json);
            }
        }
    }

    @Nested
    class WhenUpdatingStandingInstruction {
        @Test
        void throwExceptionWhenJsonIsBlankOrNull() {
            assertThrowsException(InvalidJsonException.class, null);
        }
    }

    private JsonCommand command(final JsonObject jsonObject) {
        final String json = jsonObject == null ? "" : jsonObject.toString();
        final JsonElement parsedCommand = fromApiJsonHelper.parse(json);

        return JsonCommand.from(json, parsedCommand, fromApiJsonHelper, null,
            null, null, null, null, null, null, null, null, null, null, null,
            null, null);
    }

    private JsonObject baseRequest() {   
        return StandingInstructionRequestBuilder.base().build();
    }

    private JsonObject periodicRequest() {
        JsonObject json = baseRequest();
        json.addProperty(StandingInstructionApiConstants.nameParamName, "PERIODIC TEST");
        json.addProperty(AccountDetailConstants.transferTypeParamName, 1);
        json.addProperty(AccountDetailConstants.fromAccountTypeParamName, 2);
        json.addProperty(AccountDetailConstants.toAccountTypeParamName, 2);
        json.addProperty(StandingInstructionApiConstants.priorityParamName, 1);
        json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 1);
        json.addProperty(StandingInstructionApiConstants.statusParamName, 1);
        json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 1);
        json.addProperty(StandingInstructionApiConstants.validTillParamName, "07 May 2027");
        return json;
    }

    private JsonObject duesRequest() {
        JsonObject json = periodicRequest();
        json.addProperty(StandingInstructionApiConstants.nameParamName, "DUES TEST");
        json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 2);
        return json;
    }

    private JsonObject loanRepaymentRequest() {
        JsonObject json = periodicRequest();
        json.addProperty(StandingInstructionApiConstants.nameParamName, "LOAN REPAYMENT TEST");
        json.addProperty(AccountDetailConstants.transferTypeParamName, 2);
        json.addProperty(AccountDetailConstants.toAccountTypeParamName, 1);
        json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 2);
        json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 1);
        return json;
    }

    private JsonObject accountTransferRequest() {
        JsonObject json = periodicRequest(); 
        json.addProperty(StandingInstructionApiConstants.nameParamName, "ACCOUNT TRANSFER TEST");
        json.addProperty(AccountDetailConstants.toAccountIdParamName, 2);
        return json;
    }

    private void assertValidation(final JsonObject json, final String parameter, final String reason) {
        final String expectedCode = STANDING_INSTRUCTION_MSG_BASE + parameter + "." + reason;
        
        PlatformApiDataValidationException ex = assertThrows(PlatformApiDataValidationException.class, () -> {
            this.standingInstructionDataValidator.validateForCreate(command(json));
        });

        boolean hasError = ex.getErrors().stream().anyMatch(error -> 
            parameter.equals(error.getParameterName()) &&
            expectedCode.equals(error.getUserMessageGlobalisationCode()));

        assertTrue(hasError);
    }
    
    private void assertValidationSuccess(JsonObject json) {
        assertDoesNotThrow(() ->
            standingInstructionDataValidator.validateForCreate(command(json)));
    }

    private void assertRange(JsonObject json, String param) {
        assertValidation(json, param, MSG_CODE_INVALID_RANGE);
    }

    private void assertBlank(JsonObject json, String param) {
        json.remove(param);
        assertValidation(json, param, MSG_CODE_CANNOT_BE_BLANK);
    }

    private void assertThrowsException(Class<? extends Throwable> exceptionClass, JsonObject json) {
        assertThrows(exceptionClass, () -> 
            this.standingInstructionDataValidator.validateForCreate(command(json)));
    }

    private static class StandingInstructionRequestBuilder {
    
        private final JsonObject json = new JsonObject();
    
        public static StandingInstructionRequestBuilder base() {
            return new StandingInstructionRequestBuilder()
                .locale("en")
                .dateFormat("dd MMMM yyyy")
                .fromOfficeId(1)
                .fromClientId(1)
                .fromAccountId(1)
                .fromAccountType(1)
                .toOfficeId(1)
                .toClientId(1)
                .toAccountId(1)
                .toAccountType(1)
                .transferType(4)
                .name("BASE TEST")
                .priority(5)
                .instructionType(3)
                .status(3)
                .validFrom("08 May 2026")
                .validTill("07 May 2027")
                .recurrenceType(3)
                .amount(new BigDecimal("10.00"))
                .recurrenceFrequency(2)
                .recurrenceInterval(1)
                .recurrenceOnMonthDay("08 May")
                .monthDayFormat("dd MMMM");
        }
    
        public StandingInstructionRequestBuilder locale(String v) {
            json.addProperty(AccountDetailConstants.localeParamName, v);
            return this;
        }
    
        public StandingInstructionRequestBuilder dateFormat(String v) {
            json.addProperty(AccountDetailConstants.dateFormatParamName, v);
            return this;
        }
    
        public StandingInstructionRequestBuilder fromOfficeId(Integer v) {
            json.addProperty(AccountDetailConstants.fromOfficeIdParamName, v);
            return this;
        }
    
        public StandingInstructionRequestBuilder fromClientId(Integer v) {
            json.addProperty(AccountDetailConstants.fromClientIdParamName, v);
            return this;
        }
    
        public StandingInstructionRequestBuilder fromAccountId(Integer v) {
            json.addProperty(AccountDetailConstants.fromAccountIdParamName, v);
            return this;
        }
    
        public StandingInstructionRequestBuilder fromAccountType(Integer v) {
            json.addProperty(AccountDetailConstants.fromAccountTypeParamName, v);
            return this;
        }
    
        public StandingInstructionRequestBuilder toOfficeId(Integer v) {
            json.addProperty(AccountDetailConstants.toOfficeIdParamName, v);
            return this;
        }
    
        public StandingInstructionRequestBuilder toClientId(Integer v) {
            json.addProperty(AccountDetailConstants.toClientIdParamName, v);
            return this;
        }
    
        public StandingInstructionRequestBuilder toAccountId(Integer v) {
            json.addProperty(AccountDetailConstants.toAccountIdParamName, v);
            return this;
        }
    
        public StandingInstructionRequestBuilder toAccountType(Integer v) {
            json.addProperty(AccountDetailConstants.toAccountTypeParamName, v);
            return this;
        }
    
        public StandingInstructionRequestBuilder transferType(Integer v) {
            json.addProperty(AccountDetailConstants.transferTypeParamName, v);
            return this;
        }
    
        public StandingInstructionRequestBuilder name(String v) {
            json.addProperty(StandingInstructionApiConstants.nameParamName, v);
            return this;
        }
    
        public StandingInstructionRequestBuilder priority(Integer v) {
            json.addProperty(StandingInstructionApiConstants.priorityParamName, v);
            return this;
        }
    
        public StandingInstructionRequestBuilder instructionType(Integer v) {
            json.addProperty(StandingInstructionApiConstants.instructionTypeParamName, v);
            return this;
        }
    
        public StandingInstructionRequestBuilder status(Integer v) {
            json.addProperty(StandingInstructionApiConstants.statusParamName, v);
            return this;
        }
    
        public StandingInstructionRequestBuilder validFrom(String v) {
            json.addProperty(StandingInstructionApiConstants.validFromParamName, v);
            return this;
        }
    
        public StandingInstructionRequestBuilder validTill(String v) {
            json.addProperty(StandingInstructionApiConstants.validTillParamName, v);
            return this;
        }
    
        public StandingInstructionRequestBuilder recurrenceType(Integer v) {
            json.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, v);
            return this;
        }
    
        public StandingInstructionRequestBuilder amount(BigDecimal v) {
            json.addProperty(StandingInstructionApiConstants.amountParamName, v);
            return this;
        }
    
        public StandingInstructionRequestBuilder toAccountIdOnly(Integer v) {
            json.addProperty(AccountDetailConstants.toAccountIdParamName, v);
            return this;
        }
    
        public JsonObject build() {
            return json.deepCopy();
        }
    }
}

