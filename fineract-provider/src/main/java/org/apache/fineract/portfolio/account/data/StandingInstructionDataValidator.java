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

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.account.AccountDetailConstants;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.account.api.StandingInstructionApiConstants;
import org.apache.fineract.portfolio.account.domain.AccountTransferRecurrenceType;
import org.apache.fineract.portfolio.account.domain.AccountTransferStandingInstruction;
import org.apache.fineract.portfolio.account.domain.AccountTransferType;
import org.apache.fineract.portfolio.account.domain.StandingInstructionType;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StandingInstructionDataValidator {

    private final FromJsonHelper fromApiJsonHelper;
    private final AccountTransfersDetailDataValidator accountTransfersDetailDataValidator;
    private static final Set<String> CREATE_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(AccountDetailConstants.localeParamName,
            AccountDetailConstants.dateFormatParamName, AccountDetailConstants.fromOfficeIdParamName,
            AccountDetailConstants.fromClientIdParamName, AccountDetailConstants.fromAccountTypeParamName,
            AccountDetailConstants.fromAccountIdParamName, AccountDetailConstants.toOfficeIdParamName,
            AccountDetailConstants.toClientIdParamName, AccountDetailConstants.toAccountTypeParamName,
            AccountDetailConstants.toAccountIdParamName, AccountDetailConstants.transferTypeParamName,
            StandingInstructionApiConstants.priorityParamName, StandingInstructionApiConstants.instructionTypeParamName,
            StandingInstructionApiConstants.statusParamName, StandingInstructionApiConstants.amountParamName,
            StandingInstructionApiConstants.validFromParamName, StandingInstructionApiConstants.validTillParamName,
            StandingInstructionApiConstants.recurrenceTypeParamName, StandingInstructionApiConstants.recurrenceFrequencyParamName,
            StandingInstructionApiConstants.recurrenceIntervalParamName, StandingInstructionApiConstants.recurrenceOnMonthDayParamName,
            StandingInstructionApiConstants.nameParamName, StandingInstructionApiConstants.monthDayFormatParamName));

    private static final Set<String> UPDATE_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(AccountDetailConstants.localeParamName,
            AccountDetailConstants.dateFormatParamName, StandingInstructionApiConstants.priorityParamName,
            StandingInstructionApiConstants.instructionTypeParamName, StandingInstructionApiConstants.statusParamName,
            StandingInstructionApiConstants.amountParamName, StandingInstructionApiConstants.validFromParamName,
            StandingInstructionApiConstants.validTillParamName, StandingInstructionApiConstants.recurrenceTypeParamName,
            StandingInstructionApiConstants.recurrenceFrequencyParamName, StandingInstructionApiConstants.recurrenceIntervalParamName,
            StandingInstructionApiConstants.recurrenceOnMonthDayParamName, StandingInstructionApiConstants.monthDayFormatParamName));

    @Autowired
    public StandingInstructionDataValidator(final FromJsonHelper fromApiJsonHelper,
            final AccountTransfersDetailDataValidator accountTransfersDetailDataValidator) {
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.accountTransfersDetailDataValidator = accountTransfersDetailDataValidator;
    }

    public void validateForCreate(final JsonCommand command) {
        final String json = command.json();

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, CREATE_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(StandingInstructionApiConstants.STANDING_INSTRUCTION_RESOURCE_NAME);
        this.accountTransfersDetailDataValidator.validate(command, baseDataValidator);

        final JsonElement element = command.parsedJson();

        final Integer transferType = this.fromApiJsonHelper.extractIntegerNamed(AccountDetailConstants.transferTypeParamName, element,
                Locale.getDefault());
        baseDataValidator.reset().parameter(AccountDetailConstants.transferTypeParamName).value(transferType).notNull().inMinMaxRange(1, 3);

        final String name = this.fromApiJsonHelper.extractStringNamed(StandingInstructionApiConstants.nameParamName, element);
        baseDataValidator.reset().parameter(StandingInstructionApiConstants.nameParamName).value(name).notBlank();

        final Integer priority = this.fromApiJsonHelper.extractIntegerNamed(StandingInstructionApiConstants.priorityParamName, element,
                Locale.getDefault());
        baseDataValidator.reset().parameter(StandingInstructionApiConstants.priorityParamName).value(priority).notNull().inMinMaxRange(1,
                4);

        final Integer instructionType = this.fromApiJsonHelper.extractIntegerNamed(StandingInstructionApiConstants.instructionTypeParamName,
                element, Locale.getDefault());
        baseDataValidator.reset().parameter(StandingInstructionApiConstants.instructionTypeParamName).value(instructionType).notNull()
                .inMinMaxRange(1, 2);

        final Integer status = this.fromApiJsonHelper.extractIntegerNamed(StandingInstructionApiConstants.statusParamName, element,
                Locale.getDefault());
        baseDataValidator.reset().parameter(StandingInstructionApiConstants.statusParamName).value(status).notNull().inMinMaxRange(1, 2);

        final LocalDate validFrom = this.fromApiJsonHelper.extractLocalDateNamed(StandingInstructionApiConstants.validFromParamName,
                element);
        baseDataValidator.reset().parameter(StandingInstructionApiConstants.validFromParamName).value(validFrom).notNull();

        final LocalDate validTill = this.fromApiJsonHelper.extractLocalDateNamed(StandingInstructionApiConstants.validTillParamName,
                element);
        baseDataValidator.reset().parameter(StandingInstructionApiConstants.validTillParamName).value(validTill)
                .validateDateAfter(validFrom);

        Integer recurrenceType = this.fromApiJsonHelper.extractIntegerNamed(StandingInstructionApiConstants.recurrenceTypeParamName,
                element, Locale.getDefault());
        baseDataValidator.reset().parameter(StandingInstructionApiConstants.recurrenceTypeParamName).value(recurrenceType).notNull()
                .inMinMaxRange(1, 2);

        final Integer recurrenceFrequency = this.fromApiJsonHelper
                .extractIntegerNamed(StandingInstructionApiConstants.recurrenceFrequencyParamName, element, Locale.getDefault());
        final Integer recurrenceInterval = this.fromApiJsonHelper
                .extractIntegerNamed(StandingInstructionApiConstants.recurrenceIntervalParamName, element, Locale.getDefault());

        boolean isPeriodicRecurrenceType = recurrenceType != null
                && AccountTransferRecurrenceType.fromInt(recurrenceType).isPeriodicRecurrence();
        if (isPeriodicRecurrenceType) {
<<<<<<< HEAD
            baseDataValidator.reset().parameter(StandingInstructionApiConstants.recurrenceFrequencyParamName).value(recurrenceFrequency).notNull().inMinMaxRange(0, 3);
            baseDataValidator.reset().parameter(StandingInstructionApiConstants.recurrenceIntervalParamName).value(recurrenceInterval).notNull().integerGreaterThanZero();
            MonthDay monthDay = null;

            if (recurrenceFrequency != null) {
=======
            baseDataValidator.reset().parameter(StandingInstructionApiConstants.recurrenceFrequencyParamName).value(recurrenceFrequency)
                    .notNull();
            baseDataValidator.reset().parameter(StandingInstructionApiConstants.recurrenceIntervalParamName).value(recurrenceInterval)
                    .notNull();

            MonthDay monthDay = null;

            if (recurrenceFrequency != null) {
                baseDataValidator.reset().parameter(StandingInstructionApiConstants.recurrenceFrequencyParamName).value(recurrenceFrequency)
                        .inMinMaxRange(0, 3);

>>>>>>> 95b7c92fcc283f8db9b63a3ebe4a74890364ea82
                PeriodFrequencyType frequencyType = PeriodFrequencyType.fromInt(recurrenceFrequency);
                if (frequencyType.isMonthly() || frequencyType.isYearly()) {
                    final String monthDayFormat = this.fromApiJsonHelper
                            .extractStringNamed(StandingInstructionApiConstants.monthDayFormatParamName, element);
                    baseDataValidator.reset().parameter(StandingInstructionApiConstants.monthDayFormatParamName).value(monthDayFormat)
                            .notBlank();

                    final String monthDayStr = this.fromApiJsonHelper
                            .extractStringNamed(StandingInstructionApiConstants.recurrenceOnMonthDayParamName, element);
                    baseDataValidator.reset().parameter(StandingInstructionApiConstants.recurrenceOnMonthDayParamName).value(monthDayStr)
                            .notBlank();

                    if (StringUtils.isNotBlank(monthDayStr) && StringUtils.isNotBlank(monthDayFormat)) {
                        try {
                            monthDay = this.fromApiJsonHelper
                                    .extractMonthDayNamed(StandingInstructionApiConstants.recurrenceOnMonthDayParamName, element);
                        } catch (Exception e) {
                            baseDataValidator.reset().parameter(StandingInstructionApiConstants.recurrenceOnMonthDayParamName)
                                    .failWithCode("invalid.month.day.format");
                        }
                    }
                }
            }

            if (validFrom != null && validTill != null && recurrenceFrequency != null && recurrenceInterval != null) {
                LocalDate minValidTill = null;
                PeriodFrequencyType frequencyType = PeriodFrequencyType.fromInt(recurrenceFrequency);

                if (frequencyType.isDaily()) {
                    minValidTill = validFrom.plusDays(recurrenceInterval);
                } else if (frequencyType.isWeekly()) {
                    minValidTill = validFrom.plusWeeks(recurrenceInterval);
                } else if (monthDay != null) {
                    minValidTill = monthDay.atYear(validFrom.getYear());
                    if (!minValidTill.isAfter(validFrom)) {
                        minValidTill = frequencyType.isMonthly() ? minValidTill.plusMonths(recurrenceInterval)
                                : minValidTill.plusYears(recurrenceInterval);
                    }
                }

                if (minValidTill != null && !validTill.isBefore(validFrom) && validTill.isBefore(minValidTill)) {
                    baseDataValidator.reset().parameter(StandingInstructionApiConstants.validTillParamName).value(validTill)
                            .failWithCode("must.not.be.before.first.execution.date");
                }
            }
        }

        final BigDecimal amount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(StandingInstructionApiConstants.amountParamName,
                element);

        boolean isFixedInstructionType = instructionType != null && StandingInstructionType.fromInt(instructionType).isFixedAmoutTransfer();
        if (isFixedInstructionType && isPeriodicRecurrenceType) {
            baseDataValidator.reset().parameter(StandingInstructionApiConstants.amountParamName).value(amount).notNull().positiveAmount();
        }

        boolean isDuesInstructionType = instructionType != null && StandingInstructionType.fromInt(instructionType).isDuesAmoutTransfer();
        if (isDuesInstructionType && amount != null) {
            baseDataValidator.reset().parameter(StandingInstructionApiConstants.amountParamName)
<<<<<<< HEAD
                .failWithCode("not.allowed.for.dues.instruction");
=======
                    .failWithCode("not.allowed.for.dues.instruction");
>>>>>>> 95b7c92fcc283f8db9b63a3ebe4a74890364ea82
        }

        boolean isAsPerDuesRecurrenceType = recurrenceType != null
                && AccountTransferRecurrenceType.fromInt(recurrenceType).isDuesRecurrence();
        if (transferType != null && instructionType != null && recurrenceType != null) {
            AccountTransferType accountTransferType = AccountTransferType.fromInt(transferType);
            if (accountTransferType.isAccountTransfer()) {
                if (isDuesInstructionType) {
                    baseDataValidator.reset().parameter(StandingInstructionApiConstants.instructionTypeParamName)
                            .failWithCode("dues.not.allowed.for.account.transfer");
                }
                if (isAsPerDuesRecurrenceType) {
                    baseDataValidator.reset().parameter(StandingInstructionApiConstants.recurrenceTypeParamName)
                            .failWithCode("as.per.dues.not.allowed.for.account.transfer");
                }
            }

            if (accountTransferType.isLoanRepayment() && isFixedInstructionType && isAsPerDuesRecurrenceType) {
                baseDataValidator.reset().parameter(StandingInstructionApiConstants.recurrenceTypeParamName)
                        .failWithCode("as.per.dues.not.allowed.with.fixed.amount");
            }
        }

        String errorCode = null;
        if (transferType != null) {
            AccountTransferType accountTransferType = AccountTransferType.fromInt(transferType);
            final Integer fromAccountType = this.fromApiJsonHelper
                    .extractIntegerSansLocaleNamed(AccountDetailConstants.fromAccountTypeParamName, element);
            final Integer toAccountType = this.fromApiJsonHelper
                    .extractIntegerSansLocaleNamed(AccountDetailConstants.toAccountTypeParamName, element);
            if (fromAccountType != null && toAccountType != null) {
                PortfolioAccountType fromPortfolioAccountType = PortfolioAccountType.fromInt(fromAccountType);
                PortfolioAccountType toPortfolioAccountType = PortfolioAccountType.fromInt(toAccountType);
                if (accountTransferType.isAccountTransfer() && (PortfolioAccountType.LOAN.equals(fromPortfolioAccountType)
                        || PortfolioAccountType.LOAN.equals(toPortfolioAccountType))) {
                    errorCode = "account.transfer.is.not.allowed.for.loan.accounts";
                } else if (accountTransferType.isLoanRepayment() && (PortfolioAccountType.LOAN.equals(fromPortfolioAccountType)
                        || PortfolioAccountType.SAVINGS.equals(toPortfolioAccountType))) {
                    errorCode = "is.not.a.valid.loan.repayment";
                }

                if (errorCode != null) {
                    baseDataValidator.reset().parameter(AccountDetailConstants.transferTypeParamName).failWithCode(errorCode);
                }

                if (accountTransferType.isAccountTransfer() && PortfolioAccountType.SAVINGS.equals(fromPortfolioAccountType)
                        && PortfolioAccountType.SAVINGS.equals(toPortfolioAccountType)) {

                    final Long fromAccountId = this.fromApiJsonHelper.extractLongNamed(AccountDetailConstants.fromAccountIdParamName,
                            element);
                    final Long toAccountId = this.fromApiJsonHelper.extractLongNamed(AccountDetailConstants.toAccountIdParamName, element);
                    final Long fromOfficeId = this.fromApiJsonHelper.extractLongNamed(AccountDetailConstants.fromOfficeIdParamName,
                            element);
                    final Long toOfficeId = this.fromApiJsonHelper.extractLongNamed(AccountDetailConstants.toOfficeIdParamName, element);

                    if (fromAccountId != null && toAccountId != null && fromAccountId.equals(toAccountId) && fromOfficeId != null
                            && toOfficeId != null && fromOfficeId.equals(toOfficeId)) {

                        baseDataValidator.reset().parameter(AccountDetailConstants.toAccountIdParamName)
                                .failWithCode("transfer.to.same.account.not.allowed");
                    }
                }
            }
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

<<<<<<< HEAD
    public void validateForUpdate(final JsonCommand command, final AccountTransferStandingInstruction existingStandingInstruction) {
=======
    public void validateForUpdate(final JsonCommand command, final AccountTransferStandingInstruction accountTransferStandingInstruction) {
>>>>>>> 95b7c92fcc283f8db9b63a3ebe4a74890364ea82
        final String json = command.json();

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, UPDATE_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(StandingInstructionApiConstants.STANDING_INSTRUCTION_RESOURCE_NAME);

        final JsonElement element = command.parsedJson();
<<<<<<< HEAD
=======
        if (this.fromApiJsonHelper.parameterExists(StandingInstructionApiConstants.validFromParamName, element)) {
            final LocalDate validFrom = this.fromApiJsonHelper.extractLocalDateNamed(StandingInstructionApiConstants.validFromParamName,
                    element);
            baseDataValidator.reset().parameter(StandingInstructionApiConstants.validFromParamName).value(validFrom).notNull();
        }
>>>>>>> 95b7c92fcc283f8db9b63a3ebe4a74890364ea82

        if (this.fromApiJsonHelper.parameterExists(StandingInstructionApiConstants.nameParamName, element)) {
            final String name = this.fromApiJsonHelper.extractStringNamed(StandingInstructionApiConstants.nameParamName, element);
            baseDataValidator.reset().parameter(StandingInstructionApiConstants.nameParamName).value(name).notNull();
        }

        if (this.fromApiJsonHelper.parameterExists(StandingInstructionApiConstants.priorityParamName, element)) {
            final Integer priority = this.fromApiJsonHelper.extractIntegerNamed(StandingInstructionApiConstants.priorityParamName, element,
                    Locale.getDefault());
            baseDataValidator.reset().parameter(StandingInstructionApiConstants.priorityParamName).value(priority).notNull()
                    .inMinMaxRange(1, 4);
        }

        Integer instructionType = existingStandingInstruction.getInstructionType();
        if (this.fromApiJsonHelper.parameterExists(StandingInstructionApiConstants.instructionTypeParamName, element)) {
            instructionType = this.fromApiJsonHelper
                    .extractIntegerNamed(StandingInstructionApiConstants.instructionTypeParamName, element, Locale.getDefault());
<<<<<<< HEAD
            baseDataValidator.reset().parameter(StandingInstructionApiConstants.instructionTypeParamName).value(instructionType)
                    .notNull().inMinMaxRange(1, 2);
            
            if (instructionType != null) {
                AccountTransferType existingTransferType = AccountTransferType.fromInt(
                    existingStandingInstruction.getAccountTransferDetails().getTransferType());
                StandingInstructionType newInstructionType = StandingInstructionType.fromInt(instructionType);
                if (existingTransferType.isAccountTransfer() && newInstructionType.isDuesAmountTransfer()) {
                    baseDataValidator.reset().parameter(StandingInstructionApiConstants.instructionTypeParamName)
                        .failWithCode("dues.not.allowed.for.account.transfer");
                }
            }
=======
            baseDataValidator.reset().parameter(StandingInstructionApiConstants.instructionTypeParamName).value(instructionType).notNull()
                    .inMinMaxRange(1, 2);
>>>>>>> 95b7c92fcc283f8db9b63a3ebe4a74890364ea82
        }

        if (this.fromApiJsonHelper.parameterExists(StandingInstructionApiConstants.statusParamName, element)) {
            final Integer status = this.fromApiJsonHelper.extractIntegerNamed(StandingInstructionApiConstants.statusParamName, element,
                    Locale.getDefault());
            baseDataValidator.reset().parameter(StandingInstructionApiConstants.statusParamName).value(status).notNull().inMinMaxRange(1,
                    2);
        }

        LocalDate validFrom = existingStandingInstruction.getValidFrom();
        if (this.fromApiJsonHelper.parameterExists(StandingInstructionApiConstants.validFromParamName, element)) {
            validFrom = this.fromApiJsonHelper.extractLocalDateNamed(StandingInstructionApiConstants.validFromParamName,element);
            baseDataValidator.reset().parameter(StandingInstructionApiConstants.validFromParamName).value(validFrom).notNull();

            LocalDate existingValidTill = existingStandingInstruction.getValidTill();
            if (validFrom != null && existingValidTill != null && validFrom.isAfter(existingValidTill)
                    && !this.fromApiJsonHelper.parameterExists(StandingInstructionApiConstants.validTillParamName, element)) {
                baseDataValidator.reset().parameter(validFromParamName)
                    .failWithCode("must.be.before.existing.valid.till");
            }
        }

        if (this.fromApiJsonHelper.parameterExists(StandingInstructionApiConstants.validTillParamName, element)) {
            final LocalDate validTill = this.fromApiJsonHelper.extractLocalDateNamed(StandingInstructionApiConstants.validTillParamName,
                    element);
            baseDataValidator.reset().parameter(StandingInstructionApiConstants.validTillParamName).value(validTill).notNull();

            if (validTill != null) {
                if (validFrom != null) {
                    baseDataValidator.reset().parameter(StandingInstructionApiConstants.validTillParamName).value(validTill)
                        .validateDateAfter(validFrom);
                }

                if (existingStandingInstruction.lastRunDate() != null && validTill.isBefore(existingStandingInstruction.lastRunDate())) {
                    baseDataValidator.reset().parameter(StandingInstructionApiConstants.validTillParamName)
                        .value(validTill).failWithCode("cannot.be.before.last.run.date");
                }
            }
        }

        Integer recurrenceType = existingStandingInstruction.getRecurrenceType();
        if (this.fromApiJsonHelper.parameterExists(StandingInstructionApiConstants.recurrenceTypeParamName, element)) {
            recurrenceType = this.fromApiJsonHelper
                    .extractIntegerNamed(StandingInstructionApiConstants.recurrenceTypeParamName, element, Locale.getDefault());
            baseDataValidator.reset().parameter(StandingInstructionApiConstants.recurrenceTypeParamName).value(recurrenceType).notNull()
                    .inMinMaxRange(1, 2);
            
            if(recurrenceType != null) {
                AccountTransferType existingTransferType = AccountTransferType.fromInt(
                    existingStandingInstruction.getAccountTransferDetails().getTransferType());
                AccountTransferRecurrenceType newRecurrenceType = AccountTransferRecurrenceType.fromInt(recurrenceType);
                if (existingTransferType.isAccountTransfer() && newRecurrenceType.isDuesRecurrence()) {
                    baseDataValidator.reset().parameter(StandingInstructionApiConstants.recurrenceTypeParamName)
                        .failWithCode("as.per.dues.not.allowed.for.account.transfer");
                }

                StandingInstructionType handledInstructionType = StandingInstructionType.fromInt(instructionType);
                if (existingTransferType.isLoanRepayment() && handledInstructionType != null && 
                        handledInstructionType.isFixedAmoutTransfer() && newRecurrenceType.isDuesRecurrence()){
                    baseDataValidator.reset().parameter(StandingInstructionApiConstants.recurrenceTypeParamName)
                        .failWithCode("as.per.dues.not.allowed.with.fixed.amount");
                }
            }
        }

        if (this.fromApiJsonHelper.parameterExists(StandingInstructionApiConstants.amountParamName, element)) {
            final BigDecimal amount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(StandingInstructionApiConstants.amountParamName, element);
            StandingInstructionType handledInstructionType = StandingInstructionType.fromInt(instructionType);
            AccountTransferRecurrenceType handledRecurrenceType = AccountTransferRecurrenceType.fromInt(recurrenceType);
            if (handledInstructionType != null && handledRecurrenceType != null &&
                    handledInstructionType.isFixedAmoutTransfer() && handledRecurrenceType.isPeriodicRecurrence()) {
                baseDataValidator.reset().parameter(StandingInstructionApiConstants.amountParamName).value(amount).positiveAmount();
            }

            if (handledInstructionType != null && handledInstructionType.isDuesAmoutTransfer() && amount != null) {
                baseDataValidator.reset().parameter(StandingInstructionApiConstants.amountParamName)
                    .failWithCode("not.allowed.for.dues.instruction");
            }
        }

        Integer recurrenceFrequency = existingStandingInstruction.getRecurrenceFrequency();
        if (this.fromApiJsonHelper.parameterExists(StandingInstructionApiConstants.recurrenceFrequencyParamName, element)) {
            recurrenceFrequency = this.fromApiJsonHelper.extractIntegerNamed(StandingInstructionApiConstants
                .recurrenceFrequencyParamName, element, Locale.getDefault());
            baseDataValidator.reset().parameter(StandingInstructionApiConstants.recurrenceFrequencyParamName).value(recurrenceFrequency)
                .inMinMaxRange(0, 3);
        }

        if (this.fromApiJsonHelper.parameterExists(StandingInstructionApiConstants.recurrenceIntervalParamName, element)) {
            final Integer recurrenceInterval = this.fromApiJsonHelper
                    .extractIntegerNamed(StandingInstructionApiConstants.recurrenceIntervalParamName, element, Locale.getDefault());
            baseDataValidator.reset().parameter(StandingInstructionApiConstants.recurrenceIntervalParamName).value(recurrenceInterval)
                    .integerGreaterThanZero();
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
