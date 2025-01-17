import { Button } from "antd";
import {
  UICompBuilder,
  numberExposingStateControl,
  Section,
  withDefault,
  withExposingConfigs,
  NumberControl,
  NameConfig,
  eventHandlerControl,
  withMethodExposing,
} from "openblocks-sdk";

const childrenMap = {
  value: numberExposingStateControl("value", 10),
  step: withDefault(NumberControl, 1),
  onEvent: eventHandlerControl([
    {
      label: "onChange",
      value: "change",
      description: "",
    },
  ]),
};

const HelloWorldCompBase = new UICompBuilder(childrenMap, (props) => {
  const currentValue = props.value.value;
  return (
    <div>
      <Button
        onClick={() => {
          props.value.onChange(currentValue - props.step);
          props.onEvent("change");
        }}
      >
        -
      </Button>
      <span style={{ padding: "0 8px" }}>{currentValue}</span>
      <Button
        onClick={() => {
          props.value.onChange(currentValue + props.step);
          props.onEvent("change");
        }}
      >
        +
      </Button>
    </div>
  );
})
  .setPropertyViewFn((children) => {
    return (
      <>
        <Section name="Basic">
          {children.value.propertyView({ label: "Initial Value" })}
          {children.step.propertyView({ label: "Step" })}
        </Section>
        <Section name="Interaction">{children.onEvent.propertyView()}</Section>
      </>
    );
  })
  .build();

const HelloWorldCompTemp = withMethodExposing(HelloWorldCompBase, [
  {
    method: {
      name: "random",
      params: [],
    },
    execute(comp) {
      comp.children.value.getView().onChange(Math.floor(Math.random() * 100));
    },
  },
]);

export default withExposingConfigs(HelloWorldCompTemp, [
  new NameConfig("value", ""),
  new NameConfig("step", ""),
]);
