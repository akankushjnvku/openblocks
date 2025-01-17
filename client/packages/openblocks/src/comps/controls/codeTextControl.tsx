import { CodeEditor } from "base/codeEditor/codeEditor";
import { CompParams } from "openblocks-core";
import { EditorContext } from "comps/editorState";
import { valueComp } from "comps/generators";
import { CompExposingContext } from "comps/generators/withContext";
import { exposingDataForAutoComplete } from "comps/utils/exposingTypes";
import { ControlPropertyViewWrapper } from "openblocks-design";
import _ from "lodash";
import { ReactNode, useContext, useMemo } from "react";
import { ControlParams } from "./controlParams";

interface CodeTextEditorProps extends ControlParams {
  enableExposingDataAutoCompletion?: boolean;
  codeText: string;
  onChange: (code: string) => void;
}

const emptyExposingData = {};

function CodeTextEditor(props: CodeTextEditorProps) {
  const { codeText, onChange, enableExposingDataAutoCompletion = false, ...params } = props;
  const compExposingData = useContext(CompExposingContext);
  const editorState = useContext(EditorContext);

  const expsoingData = useMemo(() => {
    if (enableExposingDataAutoCompletion) {
      return {
        ...exposingDataForAutoComplete(editorState?.nameAndExposingInfo(), true),
        ...compExposingData,
      };
    }
    return emptyExposingData;
  }, [compExposingData, editorState, enableExposingDataAutoCompletion]);

  return (
    <CodeEditor
      {...params}
      bordered
      value={codeText}
      codeType="Function"
      exposingData={expsoingData}
      onChange={(state) => onChange(state.doc.toString())}
      enableClickCompName={editorState.forceShowGrid}
    />
  );
}

export class CodeTextControl extends valueComp<string>("") {
  private readonly handleChange: (codeText: string) => void;

  constructor(params: CompParams<string>) {
    super(params);

    // make sure handleChange's reference only changes when the instance changes, avoid CodeEditor frequent reconfigure
    this.handleChange = _.debounce((codeText: string) => {
      this.dispatchChangeValueAction(codeText);
    }, 50);
  }

  propertyView(params: ControlParams) {
    return (
      <ControlPropertyViewWrapper {...params}>
        <CodeTextEditor onChange={this.handleChange} codeText={this.getView()} {...params} />
      </ControlPropertyViewWrapper>
    );
  }

  getPropertyView(): ReactNode {
    throw new Error("Method not implemented.");
  }
}
