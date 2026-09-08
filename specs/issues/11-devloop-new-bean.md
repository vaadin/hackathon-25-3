REPO: vaadin/flow
TITLE: Dev loop reports Stable for a new Spring bean, and the bean is not registered

---
### Description

Add a class annotated `@Component` and inject it into a view's constructor, in one change set. `apply` says:

```
compiling → runtime → Stable   (0.78s)
hot-reload: redefineClasses(4); onHotswap completed=true
```

Opening the view then fails:

```
NoSuchBeanDefinitionException: No qualifying bean of type 'com.example.Extra' available
```

A restart fixes it.

### Why it matters

The loop already handles the neighbouring case and says so in as many words:

```
restart: structural change to a Spring bean (PlatformEventRecorder): the existing proxy would not match the new class
```

So the machinery to notice exists, and a bean that did not exist before does not reach it. The exception names Spring rather than the loop, so the first guess is a missing annotation or a wrong package, not a restart nobody was told to do.

### Expected

Treat a new bean definition like a changed one: escalate to a restart and name the reason. Recognising `@Component`, `@Service`, `@Repository` and `@Configuration` on a class the running context has never seen covers it.

### Reproduce

[`10-devloop.zip`](https://github.com/vaadin/hackathon-25-3/raw/1e50ad1f28e0c668ef34f8a87d9cec8ad8ab4626/specs/issues/projects/10-devloop.zip), port 8100. Reproduced there, in four steps, from a project with three views and nothing else.

1. `mvn flow:install-dev-cli`, then `.vaadin/vaadin-dev start`
2. Create `Extra.java` as the comment in `BeanView` describes
3. Take the constructor parameter in `BeanView`
4. `.vaadin/vaadin-dev apply`, then open `/bean`

Step 4 prints `compiling → runtime → Stable (0.62s)` and `redefineClasses(2)`, and the page says

```
There was an exception while trying to navigate to 'bean' with the root cause
'org.springframework.beans.factory.NoSuchBeanDefinitionException:
No qualifying bean of type 'com.example.Extra' available'
```

`.vaadin/vaadin-dev restart`, and `/bean` answers 200.

Found on 25.3.0-beta1 with `flow-devloop-daemon` 25.3.0-beta1.

### Getting the project

[`10-devloop.zip`](https://github.com/vaadin/hackathon-25-3/raw/1e50ad1f28e0c668ef34f8a87d9cec8ad8ab4626/specs/issues/projects/10-devloop.zip), 6 KB, sources only. Unzip it and:

```
mvn spring-boot:run
```

