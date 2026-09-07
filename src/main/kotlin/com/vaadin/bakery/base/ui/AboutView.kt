package com.vaadin.bakery.base.ui

import com.vaadin.bakery.assistant.AssistantConfiguration.AssistantStatus
import com.vaadin.experimental.FeatureFlags
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Menu
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinService
import com.vaadin.flow.server.Version
import com.vaadin.flow.server.auth.AnonymousAllowed
import org.springframework.core.env.Environment

/**
 * What is actually running.
 *
 * This is the first page to open when a demo behaves strangely: the version,
 * the active profiles, the state of every feature flag this application depends
 * on, and which assistant is answering.
 *
 * It says nothing about licences. This is the full featured version, it expects
 * a Pro subscription, and Vaadin enforces that in the browser and in the
 * production build. A row here could only ever be a second, worse copy of a
 * check that already exists, and the platform offers no honest way to ask: see
 * `specs/FEEDBACK-25.3.md`.
 *
 * It is the one Kotlin file in the repository, written to exercise the Copilot
 * Kotlin support. Note for whoever edits it: the coding agent dev loop compiles
 * Java only, so a change here needs a restart rather than a hot swap.
 */
@Route("about")
@PageTitle("About")
// order is a double in the Java annotation, which Kotlin will not widen for you
@Menu(order = 90.0, title = "About", icon = "vaadin:info-circle")
@AnonymousAllowed
class AboutView(
    private val environment: Environment,
    private val assistant: AssistantStatus,
) : VerticalLayout() {

    private val flagsWeDependOn = listOf(
        "breadcrumbsComponent" to "Checkout trail and order detail",
        "switchComponent" to "Availability and lock toggles",
        "aiComponents" to "The assistant",
    )

    init {
        addClassName("about")
        add(H2(getTranslation("about.title")).apply { addClassName("page-block") })
        add(Paragraph(getTranslation("about.subtitle")))
        // Panels in a grid, so the three answers sit side by side on a wide
        // screen instead of stacking into a narrow column of bare tables.
        add(
            Div(
                panel(getTranslation("about.platform"), platformTable()),
                panel(getTranslation("about.flags"), flagTable()),
                panel(getTranslation("about.capabilities"), capabilityTable()),
            ).apply { addClassName("page-grid") },
        )
    }

    private fun panel(title: String, content: com.vaadin.flow.component.Component) =
        Div(H3(title), content).apply { addClassName("panel") }

    private fun platformTable() = Table().apply {
        addHeaderRow(getTranslation("about.what"), getTranslation("about.value"))
        addRowWithHeader("Vaadin", Version.getFullVersion())
        addRowWithHeader("Java", System.getProperty("java.version"))
        addRowWithHeader(
            getTranslation("about.profiles"),
            environment.activeProfiles.joinToString(", ").ifBlank { getTranslation("about.none") },
        )
        addRowWithHeader(
            getTranslation("about.mode"),
            if (VaadinService.getCurrent()?.deploymentConfiguration?.isProductionMode == true) {
                "production"
            } else {
                "development"
            },
        )
    }

    private fun flagTable() = Table().apply {
        addHeaderRow(getTranslation("about.flag"), getTranslation("about.state"), getTranslation("about.usedFor"))
        val flags = VaadinService.getCurrent()?.let { FeatureFlags.get(it.context) }
        flagsWeDependOn.forEach { (id, usedFor) ->
            val enabled = flags?.features?.any { it.id == id && it.isEnabled } ?: false
            addRowWithHeader(id, if (enabled) getTranslation("about.on") else getTranslation("about.off"), usedFor)
        }
    }

    private fun capabilityTable() = Div().apply {
        addClassName("about__capabilities")
        add(
            // There is one provider and it is real, so this row is either its
            // name or the word off. It is never the name of a stand in.
            capability(
                getTranslation("about.assistant"),
                assistant.describe() ?: getTranslation("about.off"),
            ),
            capability(
                getTranslation("about.observability"),
                if (environment.activeProfiles.contains("observability")) {
                    getTranslation("about.on")
                } else {
                    getTranslation("about.off")
                },
            ),
        )
    }

    private fun capability(label: String, value: String) = Div(
        Span(value).apply { element.themeList.add("badge small") },
        Span(" $label"),
    ).apply { addClassName("about__capability") }
}
