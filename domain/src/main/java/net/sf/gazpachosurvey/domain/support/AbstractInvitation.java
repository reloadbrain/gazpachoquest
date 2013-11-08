package net.sf.gazpachosurvey.domain.support;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import net.sf.gazpachosurvey.domain.core.SurveyRunning;
import net.sf.gazpachosurvey.types.InvitationStatus;

@Entity
@Table(name="invitation")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="type")
public abstract class AbstractInvitation extends AbstractPersistable {

    private static final long serialVersionUID = -9203813369476903640L;

    protected String token;

    @ManyToOne(fetch = FetchType.LAZY)
    protected SurveyRunning surveyRunning;

    @Enumerated(EnumType.STRING)
    protected InvitationStatus status;

    protected AbstractInvitation() {
        super();
    }

    protected AbstractInvitation(String token, SurveyRunning surveyRunning,
            InvitationStatus status) {
        super();
        this.token = token;
        this.surveyRunning = surveyRunning;
        this.status = status;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public SurveyRunning getSurveyRunning() {
        return surveyRunning;
    }

    public void setSurveyRunning(SurveyRunning surveyRunning) {
        this.surveyRunning = surveyRunning;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

   
}