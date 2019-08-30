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

package scouterx.webapp.model;

import lombok.Builder;
import lombok.Data;
import scouter.io.DataInputX;
import scouter.lang.step.ApiCallStep;
import scouter.lang.step.DispatchStep;
import scouter.lang.step.DumpStep;
import scouter.lang.step.HashedMessageStep;
import scouter.lang.step.MessageStep;
import scouter.lang.step.MethodStep;
import scouter.lang.step.ParameterizedMessageStep;
import scouter.lang.step.SocketStep;
import scouter.lang.step.SpanCallStep;
import scouter.lang.step.SpanStep;
import scouter.lang.step.SqlStep;
import scouter.lang.step.Step;
import scouter.lang.step.StepEnum;
import scouter.lang.step.ThreadCallPossibleStep;
import scouter.lang.step.ThreadSubmitStep;
import scouter.util.IPUtil;
import scouterx.webapp.framework.client.model.TextLoader;
import scouterx.webapp.framework.client.model.TextModel;
import scouterx.webapp.framework.client.model.TextTypeEnum;
import scouterx.webapp.model.scouter.step.SCommonSpanStep;
import scouterx.webapp.model.scouter.step.SSpanCallStep;
import scouterx.webapp.model.scouter.step.SSpanStep;

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

    public static List<ProfileStepData> toList(byte[] buff, long date, int serverId) {
        if (buff == null) {
            return null;
        }

        TextLoader textLoader = new TextLoader(serverId);

        List<Step> stepList = new ArrayList<>();
        DataInputX din = new DataInputX(buff);
        try {
            while (din.available() > 0) {
                Step step = convert(din.readStep());
                stepList.add(step);
                addToTextLoader(step, textLoader);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        TextModel.startScope();

        //load all text from dictionary
        textLoader.loadAll();

        List<ProfileStepData> profileStepDataList = new ArrayList<>();

        for (Step step : stepList) {
            profileStepDataList.add(ProfileStepData.of(step, date, serverId));
        }

        TextModel.endScope();

        return profileStepDataList;
    }

    private static Step convert(Step step) {
        if (step instanceof SpanStep) {
            return SSpanStep.of((SpanStep) step);

        } else if (step instanceof SpanCallStep) {
            return SSpanCallStep.of((SpanCallStep) step);

        } else {
            return step;
        }
    }

    public static ProfileStepData of(Step step, long date, int serverId) {
        String mainValue = getStepMainValue(step, date, serverId);
        List<String> additionalValueList = getStepAdditionalValue(step, date, serverId);

        ProfileStepData profileStepData = ProfileStepData.builder()
                .mainValue(mainValue)
                .additionalValueList(additionalValueList)
                .step(step)
                .build();

        return profileStepData;
    }

    private static String getStepMainValue(Step step, long date, int serverId) {
        String mainValue = "";

        StepEnum.Type stepType = StepEnum.Type.of(step.getStepType());
        TextTypeEnum textTypeEnum = TextTypeEnum.of(stepType.getAssociatedMainTextTypeName());

        switch (stepType) {
            case METHOD:
            case METHOD2:
                mainValue = textTypeEnum.getTextModel().getTextIfNullDefault(date, ((MethodStep) step).getHash(), serverId);
                break;
            case SQL:
            case SQL2:
            case SQL3:
                mainValue = textTypeEnum.getTextModel().getTextIfNullDefault(date, ((SqlStep) step).getHash(), serverId);
                break;
            case APICALL:
            case APICALL2:
                mainValue = textTypeEnum.getTextModel().getTextIfNullDefault(date, ((ApiCallStep) step).getHash(), serverId);
                break;
            case THREAD_SUBMIT:
                mainValue = textTypeEnum.getTextModel().getTextIfNullDefault(date, ((ThreadSubmitStep) step).getHash(), serverId);
                break;
            case HASHED_MESSAGE:
                mainValue = textTypeEnum.getTextModel().getTextIfNullDefault(date, ((HashedMessageStep) step).getHash(), serverId);
                break;
            case PARAMETERIZED_MESSAGE:
                ParameterizedMessageStep pmStep = (ParameterizedMessageStep) step;
                mainValue = pmStep.buildMessasge(textTypeEnum.getTextModel().getTextIfNullDefault(date, pmStep.getHash(), serverId));
                break;
            case DISPATCH:
                mainValue = textTypeEnum.getTextModel().getTextIfNullDefault(date, ((DispatchStep) step).getHash(), serverId);
                break;
            case THREAD_CALL_POSSIBLE:
                mainValue = textTypeEnum.getTextModel().getTextIfNullDefault(date, ((ThreadCallPossibleStep) step).getHash(), serverId);
                break;
            case SPAN:
            case SPANCALL:
                mainValue = textTypeEnum.getTextModel().getTextIfNullDefault(date, ((SCommonSpanStep) step).getHash(), serverId);
                break;
            case DUMP:
                break;
            case MESSAGE:
                mainValue = ((MessageStep) step).getMessage();
                break;
            case SOCKET:
                mainValue = IPUtil.toString(((SocketStep) step).getIpaddr());
            default:
                break;
        }
        return mainValue;
    }

    private static List<String> getStepAdditionalValue(Step step, long date, int serverId) {
        StepEnum.Type stepType = StepEnum.Type.of(step.getStepType());
        TextTypeEnum textTypeEnum = TextTypeEnum.of(stepType.getAssociatedMainTextTypeName());
        List<String> valueList = new ArrayList<>();

        switch (stepType) {
            case DUMP:
                DumpStep dumpStep = (DumpStep) step;
                for (int stackHash : dumpStep.stacks) {
                    valueList.add(TextTypeEnum.STACK_ELEMENT.getTextModel().getTextIfNullDefault(date, stackHash, serverId));
                }
                break;
            case SPAN:
            case SPANCALL:
                SCommonSpanStep spanStep = (SCommonSpanStep) step;
                String localEndpointName = TextTypeEnum.OBJECT.getTextModel().getTextIfNullDefault(date, spanStep.getLocalEndpoint().getHash(), serverId);
                String remoteEndpointName = TextTypeEnum.OBJECT.getTextModel().getTextIfNullDefault(date, spanStep.getRemoteEndpoint().getHash(), serverId);
                spanStep.getLocalEndpoint().setServiceName(localEndpointName);
                spanStep.getRemoteEndpoint().setServiceName(remoteEndpointName);
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
            case SPAN:
            case SPANCALL:
                textLoader.addTextHash(textTypeEnum, ((SCommonSpanStep) step).getHash());
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
                    textLoader.addTextHash(TextTypeEnum.STACK_ELEMENT, stackHash);
                }
                break;

            default:
                break;
        }
    }
}
