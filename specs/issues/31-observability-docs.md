REPO: vaadin/docs
TITLE: Three things the Observability Kit pages do not say

---
### Description

The kit itself works exactly as advertised: one starter, three properties, no agent, 22 `vaadin_*` metric families labelled by route on the first run. Three things around it are missing.

**Nothing warns that a scraper cannot log in.** The setup page says to expose `/actuator/prometheus` through the management properties, which is necessary and not sufficient in a Vaadin application: the Vaadin security chain accepts every request, so an unauthenticated scrape is answered with a 302 to the login view. Prometheus then collects a login form every five seconds and the dashboard is empty with nothing anywhere saying why. The fix is a security chain of its own for `/actuator/**`, with HTTP Basic, stateless, ordered ahead of the Vaadin one. A worked example of that chain on the page would save every project the same hour.

**Interaction Insights answers with an empty list and no explanation.** `/actuator/vaadin/observability` returns `{"schemaVersion":1,"instrumentation":"active","insights":[]}` after ordinary use. Nothing says what makes an interaction slow enough to be recorded, whether the threshold is configurable, or how long a window is kept. So there is no way to tell a working feature with nothing to report from a feature that is not reporting, which is the only question somebody has on first use.

**The metric list does not match what is served.** The material describes a UI state size gauge per route as the way to find a view that leaks server side state. No metric of that shape appeared in the 22 families the endpoint served. Either it is not there, or it is named something a reader cannot guess. A published list of metric names with their labels would settle it.

### Expected

The security example, the insight threshold, and a metric list.

### Reproduce

```
./mvnw spring-boot:run -Pobservability -Dspring-boot.run.profiles=observability
curl -s localhost:8080/actuator/prometheus            # 302 to the login view
curl -s -u admin:... localhost:8080/actuator/prometheus | grep '^# TYPE vaadin'
curl -s -u admin:... localhost:8080/actuator/vaadin/observability
```

Found on 25.3.0-beta1 with observability-kit 5.0.0-beta1.
