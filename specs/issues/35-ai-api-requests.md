REPO: vaadin/flow
TITLE: Three things the AI API cannot do that an application needs

---
### Description

**Tools of an application's own.** `withTools(Object...)` fills an opaque array that only a provider understanding annotated objects can read, and the framework agnostic path is `withController(AIController)`, one controller per orchestrator. An application that wants its own two or three actions alongside a form controller has to write a controller that wraps another controller and merges the tool lists. Ours is forty lines and exists only for that.

A `withTool(ToolSpec)` that takes the same shape the provider already understands would remove it.

**A field cannot keep its id.** `FormAIController` mints a random UUID per field on every run and puts the label in the description, so the ids in a recorded conversation mean nothing the next time. Every other Vaadin integration identifies a component by the id the application set with `setId`. Honouring it when there is one would make a turn reproducible and a log readable.

**Nothing wires a Spring AI `ChatModel` to an `LLMProvider`.** `vaadin-ai-core-flow` ships `SpringAILLMProvider` and `spring-ai-starter-model-openai` publishes a `ChatModel` bean, and a Spring Boot application with both still has to introduce them to each other. Every Spring application that uses the AI components writes the same small configuration class.

### Why it matters

None of the three is a defect and all three are the same shape: the API is one step away from the thing an application actually assembles.

### Expected

`withTool(ToolSpec)`, respect for `setId` on a field, and an auto configuration behind `vaadin.ai.enabled` that builds the provider when a `ChatModel` is present.

Found on 25.3.0-beta1.
