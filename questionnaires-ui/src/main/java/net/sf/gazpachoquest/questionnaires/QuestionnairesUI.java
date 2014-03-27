package net.sf.gazpachoquest.questionnaires;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.servlet.ServletException;

import net.sf.gazpachoquest.questionnaires.views.QuestionnairView;
import net.sf.gazpachoquest.questionnaires.views.login.LoginEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.annotations.Title;
import com.vaadin.cdi.CDIUI;
import com.vaadin.cdi.CDIViewProvider;
import com.vaadin.cdi.access.JaasAccessControl;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

@Title("Gazpacho Questionnaires")
@CDIUI
public class QuestionnairesUI extends UI {

    private static final long serialVersionUID = 1265851857862002747L;

    private static final Logger logger = LoggerFactory.getLogger(QuestionnairesUI.class);

    @Inject
    private CDIViewProvider viewProvider;

    private Navigator navigator;

    @Override
    public void init(VaadinRequest request) {
        logger.info("New Vaadin UI created");
        String invitation = request.getParameter("invitation");
        logger.info("Invitation: {} of sessions : {}", invitation);
        setSizeFull();
        GazpachoViewDisplay viewDisplay = new GazpachoViewDisplay();
        setContent(viewDisplay);

        navigator = new Navigator(this, (ViewDisplay) viewDisplay);
        navigator.addProvider(viewProvider);
        navigator.setErrorProvider(new GazpachoErrorViewProvider());

        navigator.navigateTo("login");
    }

    protected void onLogin(@Observes
    LoginEvent loginEvent) {
        try {
            JaasAccessControl.login(loginEvent.getUsername(), loginEvent.getPassword());
            navigator.navigateTo(QuestionnairView.NAME);
        } catch (ServletException e) {
            Notification.show("Error logging in", Type.ERROR_MESSAGE);
        }
    }

}
