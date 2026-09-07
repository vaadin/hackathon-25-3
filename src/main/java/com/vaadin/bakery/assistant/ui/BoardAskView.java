package com.vaadin.bakery.assistant.ui;

import com.vaadin.bakery.assistant.AssistantConfiguration.AssistantStatus;
import com.vaadin.bakery.assistant.AssistantPolicy;
import com.vaadin.bakery.assistant.BakeryDatabase;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.bakery.ordering.ui.BoardPanel;
import com.vaadin.bakery.ordering.ui.OrderBoardView;
import com.vaadin.bakery.people.Role;
import com.vaadin.flow.component.ai.grid.AIDataRow;
import com.vaadin.flow.component.ai.grid.GridAIController;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 * Asking the board a question.
 *
 * It is a panel beside the board rather than something bolted onto it, because
 * {@code GridAIController} owns a grid of its own: it does not read the state
 * of a grid that is already on screen, it fills one from a query. So the board
 * keeps its columns, its sorting and its selection, and the answer to a
 * question arrives in a grid next to it whose columns are whatever the question
 * turned out to need.
 *
 * What the query may read is decided by {@link BakeryDatabase} and not here.
 */
@Route(value = OrderBoardView.ROUTE + "/" + BoardAskView.SEGMENT, layout = OrderBoardView.class)
@PageTitle("Ask about the orders")
@RolesAllowed({ Role.ADMIN_NAME, Role.BARISTA_NAME, Role.BAKER_NAME })
public class BoardAskView extends VerticalLayout implements BoardPanel {

    /** What the board's toolbar navigates to, so the two cannot drift apart. */
    public static final String SEGMENT = "ask";

    private final Grid<AIDataRow> answers = new Grid<>();
    private final AskPanel ask;

    public BoardAskView(AssistantStatus assistant, AssistantPolicy policy, BakeryDatabase database) {
        addClassName("board-ask");

        var close = new Button(new Icon(VaadinIcon.CLOSE_SMALL), event -> close());
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        Translations.bind(close, close::setAriaLabel, "board.panel.close");
        add(close);

        answers.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);
        answers.addClassName("board-ask__answers");
        answers.setSizeFull();

        ask = new AskPanel(assistant, policy, "board.ask", "order-board", answers,
                () -> new GridAIController(answers, database));
        add(ask);
    }

    @Override
    public void close() {
        getUI().ifPresent(ui -> ui.navigate(OrderBoardView.ROUTE));
    }

    AskPanel ask() {
        return ask;
    }

    Grid<AIDataRow> answers() {
        return answers;
    }
}
