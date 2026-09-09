package com.vaadin.bakery.assistant.ui;

import com.vaadin.bakery.base.CommercialLicence;
import com.vaadin.bakery.base.ui.Fields;
import com.vaadin.bakery.assistant.AssistantConfiguration.AssistantStatus;
import com.vaadin.bakery.assistant.AssistantHistory;
import com.vaadin.bakery.assistant.AssistantPolicy;
import com.vaadin.bakery.assistant.Controllers;
import com.vaadin.bakery.assistant.OrderLineTool;
import com.vaadin.bakery.assistant.PickupSlotTool;
import com.vaadin.bakery.assistant.Prompts;
import com.vaadin.bakery.assistant.UiWork;
import com.vaadin.bakery.base.error.DomainException;
import com.vaadin.bakery.base.security.CurrentUser;
import com.vaadin.bakery.catalogue.CatalogueService;
import com.vaadin.bakery.ordering.CartLine;
import com.vaadin.bakery.ordering.Channel;
import com.vaadin.bakery.ordering.OrderService;
import com.vaadin.bakery.ordering.PickupLocationRepository;
import com.vaadin.bakery.ordering.SlotService;
import com.vaadin.bakery.ordering.ui.OrderBoardView;
import com.vaadin.bakery.ordering.ui.OrderLineEditor;
import com.vaadin.bakery.ordering.ui.SlotPicker;
import com.vaadin.bakery.people.Customer;
import com.vaadin.bakery.people.CustomerService;
import com.vaadin.bakery.people.Role;
import com.vaadin.bakery.base.i18n.Translations;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.InputMode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.component.ai.common.AIAttachment;
import com.vaadin.flow.component.ai.common.ConfidenceLevel;
import com.vaadin.flow.component.ai.common.SourceExtract;
import com.vaadin.flow.component.ai.form.FieldMarkerI18n;
import com.vaadin.flow.component.ai.form.FieldValueChangeEvent;
import com.vaadin.flow.component.ai.form.FormAIController;
import com.vaadin.flow.component.ai.form.ValueOptions;
import com.vaadin.flow.component.ai.orchestrator.AIOrchestrator;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Taking an order over the telephone.
 *
 * The barista pastes what the customer said, or photographs the note they
 * scribbled while the customer talked, and one button hands both to the
 * assistant, which fills the form. Every write goes through the binder, so a
 * value the domain refuses is reported back and the model corrects itself
 * rather than writing nonsense.
 *
 * The screen is not only for the telephone, so it does not claim to be: how
 * the order arrived is a field, and the same form takes a call, an email and
 * somebody standing at the counter dictating.
 *
 * Two things are deliberate. The policy interceptor runs before anything leaves
 * the machine, and the turn meter shows what each answer cost and whether it
 * was cut off.
 *
 * There is no second way to fill this form. A regular expression parser stood
 * beside the assistant here and was removed: a free substitute next to a
 * commercial component is the one thing `00-overview.md` rules out by name, and
 * offering both meant the screen never had to be honest about which one worked.
 * With no model, the panel says so in red and the form is filled by hand.
 */
@Route("orders/new")
@PageTitle("Counter order")
@Menu(order = 11, title = "Counter order", icon = "vaadin:cart-o")
@RolesAllowed({ Role.ADMIN_NAME, Role.BARISTA_NAME })
public class PhoneOrderView extends VerticalLayout {

    private static final Logger LOG = LoggerFactory.getLogger(PhoneOrderView.class);

    /** This panel's name in the session's assistant history. */
    private static final String PANEL = "phone-order";

    private final CatalogueService catalogue;
    private final TextField firstName = new TextField();
    private final TextField lastName = new TextField();
    private final EmailField email = Fields.email("checkout.email");
    private final TextField phone = new TextField();
    private final TextArea whatTheySaid = new TextArea();
    private final TextArea internalNote = new TextArea();
    private final ComboBox<Customer> knownCustomer = new ComboBox<>();
    private final ComboBox<Channel> channel = new ComboBox<>();
    private final OrderLineEditor editor;
    private final SlotPicker picker;
    private final TurnMeter meter = new TurnMeter();
    private final Div aiForm = new Div();
    private Upload photo;
    /** What the assistant wrote in the last turn, in the order it wrote it. */
    private final java.util.List<String> written = new java.util.ArrayList<>();
    private final Div filled = new Div();
    /** The photograph waiting to ride with the next prompt, if there is one. */
    private AIAttachment staged;
    private AIOrchestrator orchestrator;
    private final CustomerService customerSearch;
    private final SlotService slotService;
    private FormAIController controller;

    public PhoneOrderView(CatalogueService catalogue, AssistantStatus assistant, AssistantPolicy policy,
            OrderService orders, PickupLocationRepository locations, SlotService slots, CurrentUser currentUser,
            CustomerService customers, AssistantHistory history) {
        this.catalogue = catalogue;
        this.customerSearch = customers;
        this.slotService = slots;
        addClassName("phone-order");

        Translations.bind(firstName, firstName::setLabel, "checkout.firstName");
        Translations.bind(lastName, lastName::setLabel, "checkout.lastName");
        Translations.bind(phone, phone::setLabel, "checkout.phone");
        // A telephone keypad rather than a full keyboard: the field takes digits
        // and spaces, and on a phone at the counter that is the whole difference.
        phone.setInputMode(InputMode.TEL);
        firstName.setId("firstName");
        lastName.setId("lastName");
        phone.setId("phone");

        Translations.bind(knownCustomer, knownCustomer::setLabel, "assistant.knownCustomer");
        knownCustomer.setItemLabelGenerator(customer -> customer.getFullName() + " <" + customer.getEmail() + ">");
        // The model may search this list and it cannot add to it, because the
        // only values the field accepts are rows that already exist. That is
        // the whole of "the model may search but not create".
        knownCustomer.setItems(query -> customers
                .search(query.getFilter().orElse(""), query.getOffset() + query.getLimit()).stream()
                .skip(query.getOffset())
                .limit(query.getLimit()));
        knownCustomer.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                firstName.setValue(event.getValue().getFirstName());
                lastName.setValue(event.getValue().getLastName());
                email.setValue(event.getValue().getEmail() == null ? "" : event.getValue().getEmail());
                phone.setValue(event.getValue().getPhone() == null ? "" : event.getValue().getPhone());
            }
        });

        var form = new FormLayout(knownCustomer, firstName, lastName, email, phone);
        form.addClassName("phone-order__form");

        // Staff only, and the customer's own words must never land in it.
        Translations.bind(internalNote, internalNote::setLabel, "assistant.internalNote");
        internalNote.setId("internalNote");
        internalNote.setHeight("4rem");

        // How the order reached the bakery. Deliberately outside the container
        // the controller walks: the customer's words cannot know this, only the
        // person taking it can, and a model guessing it would be guessing about
        // its own conversation. ONLINE is missing because that is the channel a
        // customer uses on their own, and nobody is here to record it.
        Translations.bind(channel, channel::setLabel, "assistant.channel");
        channel.setItemLabelGenerator(value -> getTranslation(value.translationKey()));
        channel.setItems(Channel.PHONE, Channel.EMAIL, Channel.COUNTER);
        channel.setValue(Channel.PHONE);
        channel.addClassName("phone-order__channel");

        Translations.bind(whatTheySaid, whatTheySaid::setLabel, "assistant.whatTheySaid");
        // One line that grows to eight as they talk, rather than a fixed block
        // of eight that is empty most of the time. An explicit height would
        // defeat the growing, so there is none.
        whatTheySaid.setWidthFull();
        whatTheySaid.setMinRows(1);
        whatTheySaid.setMaxRows(8);
        Translations.bind(whatTheySaid, whatTheySaid::setPlaceholder, "assistant.whatTheySaid.placeholder");

        editor = new OrderLineEditor(catalogue);
        editor.addClassName("phone-order__lines");

        // No customer session cart here either: the lead time follows what
        // the barista actually typed into the editor, same as the counter.
        picker = new SlotPicker(slots, () -> editor.getLines().stream()
                .mapToInt(line -> catalogue.require(line.productId()).getLeadTimeDays())
                .max().orElse(0), locations.findByActiveTrueOrderByNameAsc());
        editor.addLinesChangeListener(event -> picker.refresh());

        var save = Translations.bindText(new Button("", event -> {
            var refusal = refuse();
            if (refusal != null) {
                Notification.show(getTranslation(refusal)).addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                orders.place(editor.getLines(), firstName.getValue(), lastName.getValue(), email.getValue(),
                        phone.getValue(), picker.getLocation(), picker.getDate(), picker.getTime(),
                        channel.getValue(), null, currentUser.get().orElse(null));
                getUI().ifPresent(ui -> ui.navigate(OrderBoardView.ROUTE));
            } catch (DomainException failure) {
                Notification.show(getTranslation(failure.translationKey(), failure.arguments()))
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }), "board.editor.save");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // One container holds everything the assistant may fill, which is what
        // the controller walks to discover the fields. What the barista pastes
        // stays outside it: it is the input, not part of the form.
        aiForm.addClassName("phone-order__ai-form");
        aiForm.add(form, editor, picker, internalNote);

        var header = new Div(Translations.bindText(new H2(), "assistant.counterOrder"), channel);
        header.addClassName("phone-order__header");

        add(header, assistantPanel(assistant, policy, history), aiForm, save, meter);
    }

    /**
     * What the barista types into, and the one control that acts on it.
     *
     * There used to be a chat box here beside the paste area, which meant the
     * screen had two places to type and no way to tell what either did: the
     * paste area was labelled and inert, and the thing that actually reached
     * the model was a message input with a Send button. Now the paste area is
     * the input, the photograph is the other input, and one button sends
     * whichever of them is filled.
     *
     * The provider is not named here any more. It is on the about page, which
     * is where a reader looks for what the build is made of, and a badge over
     * every assistant repeated it without ever being the answer to a question
     * somebody had while taking an order.
     */
    private Div assistantPanel(AssistantStatus assistant, AssistantPolicy policy, AssistantHistory history) {
        var panel = new Div();
        panel.addClassName("phone-order__assistant");

        // The controller is built whether or not there is a model behind it.
        // It is what decides which fields the model may see and what it may
        // write, and those rules are worth having, and worth asserting, on a
        // machine with no key.
        //
        // What it does need is a commercial key, and not because of what it
        // does: `FormAIController`'s constructor validates the licence over
        // HTTP, and with no key that call blocks for five minutes and then
        // throws, which takes this whole view down with it. So the question is
        // asked first, from a file rather than from the network, and the view
        // works without it minus the form filling. That is in
        // specs/FEEDBACK-25.3.md and it is why CommercialLicence exists.
        controller = CommercialLicence.isPresent() ? formController() : null;

        if (!assistant.isAvailable()) {
            // And nothing to paste into either: an input that cannot reach a
            // model is worse than no input, because it looks like one that can.
            panel.add(Assistants.unavailable(assistant));
            return panel;
        }

        // Captured while the view is being built, because a response arrives on
        // a worker thread where there is no current UI to ask for.
        var ui = UI.getCurrent();

        var messages = new MessageList();
        // A model writes markdown whether or not anybody asked it to, and a
        // message list renders text, so its emphasis arrived as asterisks
        // around the words it meant to stress.
        messages.setMarkdown(true);

        try {
            // The orchestrator itself is free. The policy hook and the turn meter
            // are the two things this application cares about most. The lines
            // ride alongside the controller as a tool of ours, because a list
            // that grows has no field to fill until it has grown.
            //
            // No input component: this one is prompted from code, so that one
            // button can carry the pasted words and the photograph together.
            orchestrator = AIOrchestrator.builder(assistant.newSession(), Prompts.of("phone-order"))
                    .withMessageList(messages)
                    // What this panel said before the barista walked away from
                    // the screen. The map is empty because attachments are not
                    // kept: see AssistantHistory.
                    .withHistory(history.of(PANEL), java.util.Map.of())
                    .withController(Controllers.of(controller, tools(UI.getCurrent())))
                    .withRequestInterceptor(policy.interceptor())
                    .withResponseListener(event -> {
                        event.getMetadata().ifPresent(metadata -> meter.record(
                                metadata.tokenUsage() == null ? 0 : metadata.tokenUsage().totalTokens(),
                                metadata.finishReason()));
                        UiWork.on(ui, this::sayIfTheSlotIsMissing);
                        if (event.getError().isEmpty()) {
                            history.keep(PANEL, orchestrator.getHistory());
                        }
                    })
                    .withAssistantName(getTranslation("app.name"))
                    .build();
            panel.add(whatTheySaid, actions(), filled, messages);
        } catch (RuntimeException unavailable) {
            // A licence the machine does not have, or the components switched
            // off. Same treatment: say so where it can be seen.
            LOG.warn("The assistant panel could not be built: {}", unavailable.getMessage());
            panel.add(Assistants.unavailable(assistant));
        }

        return panel;
    }

    /**
     * The fill button and the photograph, on one line under the paste area.
     *
     * The photograph is an upload of ours rather than the orchestrator's file
     * receiver. The receiver hands its files to the orchestrator when a user
     * submits a message through an input component, and there is no input
     * component here, so keeping the bytes ourselves is what lets one button
     * send words and a picture in the same turn. Source tracking allows one
     * attachment per prompt, which is also why the upload takes one file.
     */
    private Div actions() {
        var fill = new Button(new Icon(VaadinIcon.MAGIC), event -> fill());
        fill.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Translations.bindText(fill, "assistant.fill");

        photo = new Upload(UploadHandler.inMemory((metadata, data) -> staged = new AIAttachment(
                metadata.fileName(), metadata.contentType(), data)));
        photo.setMaxFiles(1);
        photo.setAcceptedMimeTypes("image/*");
        photo.setMaxFileSize(8 * 1024 * 1024);
        // Nothing to drop onto: the drop target is a box the size of the form,
        // and this sits on one line beside a button.
        photo.setDropAllowed(false);
        photo.addFileRemovedListener(event -> staged = null);
        photo.addClassName("phone-order__photo");

        var pick = new Button(new Icon(VaadinIcon.CAMERA));
        pick.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Translations.bindText(pick, "assistant.photo");
        photo.setUploadButton(pick);

        filled.addClassName("phone-order__filled");
        filled.setVisible(false);

        var row = new Div(fill, photo);
        row.addClassName("phone-order__actions");
        return row;
    }

    /**
     * One turn, from whatever the barista has given us.
     *
     * A photograph with no words still needs a sentence, because a prompt is
     * what the turn is made of, so an empty paste area supplies one saying to
     * read the note. Both empty is the one case that asks rather than guesses.
     */
    private void fill() {
        var said = whatTheySaid.getValue() == null ? "" : whatTheySaid.getValue().trim();
        var attachment = staged;
        if (said.isBlank() && attachment == null) {
            Notification.show(getTranslation("assistant.fill.nothing"))
                    .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            return;
        }
        // The record is of this turn, so the previous turn's is not left
        // standing next to values it no longer describes.
        written.clear();
        showWhatWasFilled();

        var message = said.isBlank() ? getTranslation("assistant.fill.fromPhoto") : said;
        if (attachment == null) {
            orchestrator.prompt(message);
            return;
        }
        orchestrator.prompt(message, java.util.List.of(attachment));
        // Sent, so it is no longer waiting. The next turn is about the next
        // note, and the same picture twice is a second bill for nothing.
        staged = null;
        photo.clearFileList();
    }

    /**
     * Which fields the assistant wrote, as a line under the button.
     *
     * The marker on each field is the platform's answer to this and it is not
     * the same answer: it clears itself the moment the barista edits the field,
     * which is exactly when somebody wants to know what the assistant had done.
     * This line is built from the controller's own change events rather than
     * from the model's account of itself, so it says what actually happened to
     * the form and it survives the first keystroke.
     *
     * Lines and the pickup are absent from it on purpose: they are not fields,
     * they arrive through tools of ours, and the tools already report them.
     */
    private void showWhatWasFilled() {
        filled.setVisible(!written.isEmpty());
        if (!written.isEmpty()) {
            filled.setText(getTranslation("assistant.filled", String.join(", ", written)));
        }
    }

    /**
     * Says so when the turn left the pickup empty.
     *
     * The slot is the one thing on this screen the model cannot write directly,
     * and a model that skips the tool still writes a sentence saying when the
     * order will be collected: watched twice, it announced a Friday at five
     * that no field held. The form is what the bakery reads, so when the lines
     * are there and the pickup is not, the screen says it out loud rather than
     * leaving the chat's account of it standing.
     */
    private Void sayIfTheSlotIsMissing() {
        if (!editor.getLines().isEmpty() && picker.getDate() == null) {
            // Longer than the default five seconds: it arrives at the end of a
            // turn the reader spent watching the form fill itself, and it is
            // the one thing on the screen that is missing rather than wrong.
            var warning = Notification.show(getTranslation("assistant.slot.missing"), 9000,
                    Notification.Position.BOTTOM_START);
            warning.addThemeVariants(NotificationVariant.LUMO_WARNING);
        }
        return null;
    }

    /**
     * The controller, and every rule the specification puts on what the model
     * may touch.
     *
     * The container it walks holds the customer form, the lines and the slot
     * picker, so a field is in scope by being on the form rather than by being
     * listed here. What is listed here is the exceptions: the one field the
     * model must never see, and the one field whose values it may search but
     * not invent.
     */
    private FormAIController formController() {
        var built = new FormAIController(aiForm);

        // AC2, first rule. A staff only note is not a field the model gets to
        // have an opinion about, and a customer's words must never land in it.
        built.ignoreField(internalNote);

        // AC2, second rule. The field offers the people the bakery already
        // knows and accepts nothing else, so "search but do not create" is a
        // property of the option set rather than an instruction in a prompt.
        built.fieldValueOptions(ValueOptions.forField(knownCustomer)
                .options((filter, limit) -> customerSearch.search(filter == null ? "" : filter,
                        limit == null ? 20 : limit))
                .itemLabelGenerator(customer -> customer.getFullName() + " <" + customer.getEmail() + ">"));

        built.describeField(firstName, "The caller's first name, as they said it.");
        built.describeField(lastName, "The caller's surname. Leave empty rather than guessing one.");
        built.describeField(email, "The caller's email, only if they actually gave one.");
        built.describeField(phone, "The caller's telephone number, digits and spaces as they said it.");
        built.describeField(knownCustomer,
                "An existing customer of the bakery. Search it before filling the name fields by hand. "
                        + "If nobody matches, leave it empty: only a person may add a customer.");

        // One line per field the model wrote, after a successful turn, on the
        // UI thread with the lock held, so it can touch components directly.
        built.addFieldValueChangeListener(event -> {
            if (event.getField() instanceof HasLabel labelled && labelled.getLabel() != null
                    && !labelled.getLabel().isBlank()) {
                written.add(labelled.getLabel());
            }
            showWhatWasFilled();
        });

        // Every value comes back with the words it was read from and how sure
        // the model was, which is what the marker popover below shows.
        built.setSourceTrackingEnabled(true);
        built.setFieldMarkerEnabled(true);
        Translations.onLocale(this, locale -> built.setFieldMarkerI18n(markerTexts(locale)));
        built.setFieldMarkerPopoverContentProvider(this::whereItCameFrom);
        return built;
    }

    /**
     * The marker's own popover content: the snippet the value was read from,
     * and how sure the model was of it. The platform supplies the badge, the
     * explanation and the revert control around this.
     */
    private com.vaadin.flow.component.Component whereItCameFrom(FieldValueChangeEvent event) {
        var content = new Div();
        content.addClassName("phone-order__source");

        var source = event.getFieldSource().orElse(null);
        if (source == null) {
            return Translations.bindText(new Span(), "assistant.source.none");
        }

        var confidence = Translations.bindText(new Span(), confidenceKey(source.confidence()));
        confidence.getElement().getThemeList().add("badge small " + confidenceTheme(source.confidence()));
        content.add(confidence);

        source.extracts().stream().map(SourceExtract::text)
                .filter(text -> text != null && !text.isBlank())
                .forEach(text -> {
                    // The quotation marks are CSS, so they follow the reader's
                    // language instead of being a literal in here.
                    var quote = new Span(text);
                    quote.addClassName("phone-order__quote");
                    content.add(quote);
                });
        if (source.extracts().isEmpty()) {
            content.add(Translations.bindText(new Span(), "assistant.source.noExtract"));
        }
        return content;
    }

    private FieldMarkerI18n markerTexts(java.util.Locale locale) {
        var texts = new FieldMarkerI18n();
        texts.setBadgeLabel(getTranslation(locale, "assistant.marker.badge"));
        texts.setBadgeTooltip(getTranslation(locale, "assistant.marker.tooltip"));
        texts.setMessage(getTranslation(locale, "assistant.marker.message"));
        texts.setRevert(getTranslation(locale, "assistant.marker.revert"));
        return texts;
    }

    private static String confidenceKey(ConfidenceLevel level) {
        return "assistant.confidence." + level.name();
    }

    private static String confidenceTheme(ConfidenceLevel level) {
        return switch (level) {
            case HIGH -> "success";
            case MEDIUM -> "contrast";
            case LOW -> "error";
        };
    }

    /** The controller, for the tests that assert on what the model may do. */
    FormAIController controller() {
        return controller;
    }

    /**
     * Ours, beside the controller's.
     *
     * The lines are here rather than on the form because a list that grows has
     * no field to fill until it has grown, and the slot is here because the
     * bakery has to be allowed to say no with a reason. Both are also invisible
     * to field discovery, which does not walk into a {@code Composite}.
     */
    private com.vaadin.flow.component.ai.provider.LLMProvider.ToolSpec[] tools(com.vaadin.flow.component.UI ui) {
        return new com.vaadin.flow.component.ai.provider.LLMProvider.ToolSpec[] {
                new OrderLineTool(catalogue, editor, ui), new PickupSlotTool(slotService, picker, ui) };
    }

    /** Exactly what the model is handed, for the tests that assert on it. */
    java.util.List<com.vaadin.flow.component.ai.provider.LLMProvider.ToolSpec> allTools() {
        return Controllers.of(controller, tools(UI.getCurrent())).getTools();
    }

    SlotPicker picker() {
        return picker;
    }

    /** One turn, for the tests that drive the assistant rather than a person. */
    void ask(String message) {
        orchestrator.prompt(message);
    }

    /** Any product the bakery really sells, for a test that needs a valid one. */
    String anyProductName() {
        return catalogue.availableProducts().getFirst().getName();
    }

    /**
     * What stops this save, as a translation key, or {@code null} when nothing
     * does. This only catches what the view itself can know before asking:
     * whether there is anything to order, a way to reach the customer, and a
     * chosen slot. Everything {@code OrderService.place} can still refuse
     * (a product not baked that day, a lead time violation, a full slot) rolls
     * back cleanly on its own, because resolving the customer and placing the
     * order now happen in the one transaction that method runs.
     */
    private String refuse() {
        if (editor.getLines().isEmpty()) {
            return "board.editor.linesRequired";
        }
        if (email.getValue() == null || email.getValue().isBlank()) {
            return "board.editor.emailRequired";
        }
        if (picker.getDate() == null || picker.getTime() == null) {
            return "ordering.slot.notChosen";
        }
        return null;
    }


    TurnMeter meter() {
        return meter;
    }

    TextField firstNameField() {
        return firstName;
    }

    TextField lastNameField() {
        return lastName;
    }

    TextField phoneField() {
        return phone;
    }

    TextArea pasteField() {
        return whatTheySaid;
    }

    OrderLineEditor editor() {
        return editor;
    }
}
