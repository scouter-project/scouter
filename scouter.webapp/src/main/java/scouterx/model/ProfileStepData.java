/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouterx.model;

import lombok.Builder;
import lombok.Data;
import scouter.io.DataInputX;
import scouter.lang.step.ApiCallStep;
import scouter.lang.step.DispatchStep;
import scouter.lang.step.DumpStep;
import scouter.lang.step.HashedMessageStep;
import scouter.lang.step.MethodStep;
import scouter.lang.step.ParameterizedMessageStep;
import scouter.lang.step.SqlStep;
import scouter.lang.step.Step;
import scouter.lang.step.StepEnum;
import scouter.lang.step.ThreadCallPossibleStep;
import scouter.lang.step.ThreadSubmitStep;
import scouterx.client.model.TextLoader;
import scouterx.client.model.TextTypeEnum;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 9. 11.
 */
@Data
@Builder
public class ProfileStepData {
    private String mainValue;
    private List<String> additionalValueList;
    private Step step;

    public static List<ProfileStepData> toList(byte[] buff, int serverId) {
        if (buff == null) {
            return null;
        }

        TextLoader textLoader = new TextLoader(serverId);

        List<Step> stepList = new ArrayList<>();
        DataInputX din = new DataInputX(buff);
        try {
            while (din.available() > 0) {
                Step step = din.readStep();
                stepList.add(step);
                addToTextLoader(step, textLoader);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //load all text from dictionary
        textLoader.loadAll();

        List<ProfileStepData> profileStepDataList = new ArrayList<>();

        for (Step step : stepList) {
            profileStepDataList.add(ProfileStepData.of(step));
        }

        return profileStepDataList;
    }

    public static ProfileStepData of(Step step) {
        String mainValue = getStepMainValue(step);
        List<String> additionalValueList = getStepAdditionalValue(step);

        ProfileStepData profileStepData = ProfileStepData.builder()
                .mainValue(mainValue)
                .additionalValueList(additionalValueList)
                .step(step)
                .build();

        return profileStepData;
    }

    private static String getStepMainValue(Step step) {
        String mainValue = "";

        StepEnum.Type stepType = StepEnum.Type.of(step.getStepType());
        TextTypeEnum textTypeEnum = TextTypeEnum.of(stepType.getAssociatedMainTextTypeName());

        switch (stepType) {
            case METHOD:
            case METHOD2:
                mainValue = textTypeEnum.getTextModel().getCachedText(((MethodStep) step).getHash());
                break;
            case SQL:
            case SQL2:
            case SQL3:
                mainValue = textTypeEnum.getTextModel().getCachedText(((SqlStep) step).getHash());
                break;
            case APICALL:
            case APICALL2:
                mainValue = textTypeEnum.getTextModel().getCachedText(((ApiCallStep) step).getHash());
                break;
            case THREAD_SUBMIT:
                mainValue = textTypeEnum.getTextModel().getCachedText(((ThreadSubmitStep) step).getHash());
                break;
            case HASHED_MESSAGE:
                mainValue = textTypeEnum.getTextModel().getCachedText(((HashedMessageStep) step).getHash());
                break;
            case PARAMETERIZED_MESSAGE:
                mainValue = textTypeEnum.getTextModel().getCachedText(((ParameterizedMessageStep) step).getHash());
                break;
            case DISPATCH:
                mainValue = textTypeEnum.getTextModel().getCachedText(((DispatchStep) step).getHash());
                break;
            case THREAD_CALL_POSSIBLE:
                mainValue = textTypeEnum.getTextModel().getCachedText(((ThreadCallPossibleStep) step).getHash());
                break;
            case DUMP:
                break;
            case MESSAGE:
            case SOCKET:
            default:
                break;
        }
        return mainValue;
    }

    private static List<String> getStepAdditionalValue(Step step) {
        StepEnum.Type stepType = StepEnum.Type.of(step.getStepType());
        TextTypeEnum textTypeEnum = TextTypeEnum.of(stepType.getAssociatedMainTextTypeName());
        List<String> valueList = new ArrayList<>();

        switch (stepType) {
            case DUMP:
                DumpStep dumpStep = (DumpStep) step;
                for (int stackHash : dumpStep.stacks) {
                    valueList.add(textTypeEnum.getTextModel().getCachedText(stackHash));
                }
                break;

            default:
                break;
        }

        return valueList;
    }

    private static void addToTextLoader(Step step, TextLoader textLoader) {
        addMainValueHashToTextLoader(step, textLoader);
        addAdditionalValueHashesToTextLoader(step, textLoader);
    }

    private static void addMainValueHashToTextLoader(Step step, TextLoader textLoader) {
        StepEnum.Type stepType = StepEnum.Type.of(step.getStepType());
        String textTypeName = stepType.getAssociatedMainTextTypeName();
        TextTypeEnum textTypeEnum = TextTypeEnum.of(textTypeName);

        switch (stepType) {
            case METHOD:
            case METHOD2:
                textLoader.addTextHash(textTypeEnum, ((MethodStep) step).getHash());
                break;
            case SQL:
            case SQL2:
            case SQL3:
                textLoader.addTextHash(textTypeEnum, ((SqlStep) step).getHash());
                break;
            case APICALL:
            case APICALL2:
                textLoader.addTextHash(textTypeEnum, ((ApiCallStep) step).getHash());
                break;
            case THREAD_SUBMIT:
                textLoader.addTextHash(textTypeEnum, ((ThreadSubmitStep) step).getHash());
                break;
            case HASHED_MESSAGE:
                textLoader.addTextHash(textTypeEnum, ((HashedMessageStep) step).getHash());
                break;
            case PARAMETERIZED_MESSAGE:
                textLoader.addTextHash(textTypeEnum, ((ParameterizedMessageStep) step).getHash());
                break;
            case DISPATCH:
                textLoader.addTextHash(textTypeEnum, ((DispatchStep) step).getHash());
                break;
            case THREAD_CALL_POSSIBLE:
                textLoader.addTextHash(textTypeEnum, ((ThreadCallPossibleStep) step).getHash());
                break;
            case DUMP:
                break;
            case MESSAGE:
            case SOCKET:
            default:
                break;
        }
    }

    private static void addAdditionalValueHashesToTextLoader(Step step, TextLoader textLoader) {
        StepEnum.Type stepType = StepEnum.Type.of(step.getStepType());
        String textTypeName = stepType.getAssociatedMainTextTypeName();
        TextTypeEnum textTypeEnum = TextTypeEnum.of(textTypeName);

        switch (stepType) {
            case DUMP:
                DumpStep dumpStep = (DumpStep) step;
                for (int stackHash : dumpStep.stacks) {
                    textLoader.addTextHash(textTypeEnum, stackHash);
                }
                break;

            default:
                break;
        }
    }
}
