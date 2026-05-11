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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StandingInstructionDataValidatorTest {    

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
    class ValidateForCreate {
        
        @Test
        void blankOrNullJsonThrowsInvalidJsonException(){
            final String nullJson = null;
            final JsonCommand command = createJsonCommand(nullJson);

            assertThrows(InvalidJsonException.class,
                () -> standingInstructionDataValidator.validateForCreate(command));
        }

        @Test
        void jsonWithInvalidParamThrowsUnsupportedParameterException() {
            final JsonObject jsonObject = createBaseJsonObjectForCreate();
            jsonObject.addProperty("invalidParam", "invalidValue");

            final String json = jsonObject.toString();
            final JsonCommand command = createJsonCommand(json);

            assertThrows(UnsupportedParameterException, 
                () -> standingInstructionDataValidator.validateForCreate(command));
        }
    }

    private JsonCommand createJsonCommand(final String json) {
        final JsonElement parsedCommand = fromApiJsonHelper.parse(json);

        return JsonCommand.from(json, parsedCommand, fromApiJsonHelper, null,
            null, null, null, null, null, null, null, null, null, null, null,
            null, null);
    }

    private JsonObject createBaseJsonObjectForUpdate() {
        JsonObject jsonObject = createBaseJsonObjectForCreate();
        jsonObject.remove(AccountDetailConstants.idParamName);
        return jsonObject;
    }

    private JsonObject createBaseJsonObjectForCreate() {   
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(AccountDetailConstants.localeParamName, "en");
        jsonObject.addProperty(AccountDetailConstants.dateFormatParamName, "dd MMMM yyyy");
        jsonObject.addProperty(AccountDetailConstants.fromOfficeIdParamName, 1);
        jsonObject.addProperty(AccountDetailConstants.fromClientIdParamName, 1);
        jsonObject.addProperty(AccountDetailConstants.fromAccountIdParamName, 1);
        jsonObject.addProperty(AccountDetailConstants.fromAccountTypeParamName, 2);
        jsonObject.addProperty(AccountDetailConstants.toOfficeIdParamName, 1);
        jsonObject.addProperty(AccountDetailConstants.toClientIdParamName, 2);
        jsonObject.addProperty(AccountDetailConstants.toAccountIdParamName, 2);
        jsonObject.addProperty(AccountDetailConstants.toAccountTypeParamName, 2);
        jsonObject.addProperty(AccountDetailConstants.transferTypeParamName, 1);
        jsonObject.addProperty(StandingInstructionApiConstants.nameParamName, "TEST FROM SAVINGS TO SAVINGS");
        jsonObject.addProperty(StandingInstructionApiConstants.priorityParamName, 1);
        jsonObject.addProperty(StandingInstructionApiConstants.instructionTypeParamName, 1);
        jsonObject.addProperty(StandingInstructionApiConstants.statusParamName, 1);
        jsonObject.addProperty(StandingInstructionApiConstants.amountParamName, 10.00);
        jsonObject.addProperty(StandingInstructionApiConstants.validFromParamName, "08 May 2026");
        jsonObject.addProperty(StandingInstructionApiConstants.validTillParamName, "08 May 2027");
        jsonObject.addProperty(StandingInstructionApiConstants.recurrenceTypeParamName, 1);
        jsonObject.addProperty(StandingInstructionApiConstants.recurrenceFrequencyParamName, 2);
        jsonObject.addProperty(StandingInstructionApiConstants.recurrenceIntervalParamName, 1);
        jsonObject.addProperty(StandingInstructionApiConstants.recurrenceOnMonthDayParamName, "08 May");
        jsonObject.addProperty(StandingInstructionApiConstants.monthDayFormatParamName, "dd MMMM");
        return jsonObject;
    }
}