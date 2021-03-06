package net.sf.gazpachoquest.questionnaire.resolver;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.el.ELException;
import javax.el.ExpressionFactory;

import net.sf.gazpachoquest.domain.core.Breadcrumb;
import net.sf.gazpachoquest.domain.core.Question;
import net.sf.gazpachoquest.domain.core.QuestionBreadcrumb;
import net.sf.gazpachoquest.domain.core.Questionnaire;
import net.sf.gazpachoquest.domain.core.QuestionnaireDefinition;
import net.sf.gazpachoquest.domain.core.Section;
import net.sf.gazpachoquest.domain.core.SectionBreadcrumb;
import net.sf.gazpachoquest.qbe.SearchParameters;
import net.sf.gazpachoquest.questionnaire.support.PageMetadataCreator;
import net.sf.gazpachoquest.questionnaire.support.PageStructure;
import net.sf.gazpachoquest.services.BreadcrumbService;
import net.sf.gazpachoquest.services.QuestionService;
import net.sf.gazpachoquest.services.QuestionnaireAnswersService;
import net.sf.gazpachoquest.services.QuestionnaireDefinitionService;
import net.sf.gazpachoquest.services.QuestionnaireService;
import net.sf.gazpachoquest.services.SectionService;
import net.sf.gazpachoquest.types.NavigationAction;
import net.sf.gazpachoquest.types.RandomizationStrategy;
import net.sf.gazpachoquest.types.RenderingMode;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.Assert;

import de.odysseus.el.util.SimpleContext;

public abstract class AbstractResolver<T extends Breadcrumb> implements PageResolver {

    private static final Logger logger = LoggerFactory.getLogger(AbstractResolver.class);

    protected static final Integer QUESTION_NUMBER_START_COUNTER = 1;

    protected static final boolean BREADCRUMB_CACHE_ENABLED = true;

    @Autowired
    private QuestionnaireAnswersService questionnaireAnswersService;
    
    @Autowired
    private BreadcrumbService breadcrumbService;

    @Autowired
    private SectionService sectionService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionnaireService questionnaireService;

    @Autowired
    @Qualifier("questionnaireDefinitionServiceImpl")
    protected QuestionnaireDefinitionService questionnaireDefinitionService;

    @Autowired
    protected PageMetadataCreator metadataCreator;

    protected final RenderingMode type;

   // @Autowired
   // @Qualifier("elFactory")
  //  protected ExpressionFactory elFactory;
    protected ExpressionFactory elFactory = ExpressionFactory.newInstance();


    protected AbstractResolver(RenderingMode type) {
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public PageStructure resolveNextPage(final Questionnaire questionnaire, final NavigationAction action) {
        Questionnaire fetchedQuestionnair = questionnaireService.findOne(questionnaire.getId());
        QuestionnaireDefinition questionnaireDefinition = fetchedQuestionnair.getQuestionnaireDefinition();
        int questionnaireId = questionnaire.getId();
        logger.debug("Fetching {} page for questionnaire {}", action.toString(), questionnaireId);

        List<Object[]> result = breadcrumbService.findLastAndPosition(questionnaireId);
        if (!type.equals(RenderingMode.ALL_IN_ONE)){
            Assert.state(result.size() <= 1, "Found more than one visible breadcrumb");
        }
        T breadcrumb = null;
        List<T> lastBreadcrumbs = new ArrayList<>();

        List<T> breadcrumbs = null;
        Integer lastBreadcrumbPosition = null;
        // First time entering the questionnaire
        if (result.isEmpty()) {
            breadcrumbs = makeBreadcrumbs(questionnaireDefinition, questionnaire);
            leaveBreakcrumbs(questionnaire, breadcrumbs);
            populateLastBreadcrumbs(lastBreadcrumbs, breadcrumbs);
        } else {
            // At least one breadcrumb
            lastBreadcrumbPosition = (Integer) result.get(0)[1];
            breadcrumb = (T) result.get(0)[0];
            if (!breadcrumb.getRenderingMode().equals(type)) {
                // Clean dirties
                breadcrumbService.deleteByExample(
                        Breadcrumb.withProps().questionnaire(Questionnaire.with().id(questionnaireId).build()).build(),
                        new SearchParameters());
                breadcrumbs = makeBreadcrumbs(questionnaireDefinition, questionnaire);
                leaveBreakcrumbs(questionnaire, breadcrumbs);
                populateLastBreadcrumbs(lastBreadcrumbs, breadcrumbs);
            } else {
                if (!type.equals(RenderingMode.ALL_IN_ONE)) {
                    lastBreadcrumbs.add(breadcrumb);
                } else {
                    for (Object[] row : result) {
                        breadcrumb = (T) row[0];
                        lastBreadcrumbs.add(breadcrumb);
                    }
                }

            }

        }
        Map<String, Object> answers = questionnaireAnswersService.findByQuestionnaire(questionnaire);
        List<T> nextBreadcrumbs = new ArrayList<>();
        if (NavigationAction.ENTERING.equals(action)) {
            nextBreadcrumbs = lastBreadcrumbs;
        } else {
            T nextBreadcrumb;
            if (NavigationAction.NEXT.equals(action)) {
                nextBreadcrumb = findNextBreadcrumb(questionnaireDefinition, questionnaire, answers, lastBreadcrumbs.get(0),
                        lastBreadcrumbPosition);
                Assert.notNull(nextBreadcrumb, "Page out of range");
                lastBreadcrumbs.get(0).setLast(Boolean.FALSE);
                breadcrumbService.save(lastBreadcrumbs.get(0));
                
                nextBreadcrumb.setLast(Boolean.TRUE);
                if (nextBreadcrumb.isNew()){
                    questionnaire.addBreadcrumb(nextBreadcrumb);
                    questionnaireService.save(questionnaire);
                }else{
                    breadcrumbService.save(nextBreadcrumb);
                }
            } else {// PREVIOUS
                nextBreadcrumb = findPreviousBreadcrumb(questionnaireDefinition, questionnaire, lastBreadcrumbs.get(0),
                        lastBreadcrumbPosition);
                Assert.notNull(nextBreadcrumb, "Page out of range");
                if (breadcrumbCacheEnable()) {
                    lastBreadcrumbs.get(0).setLast(Boolean.FALSE);
                    breadcrumbService.save(lastBreadcrumbs.get(0));
                } else {
                    // Removed displayed questions from breadcrumbs
                    /*-
                    Breadcrumb entity = Breadcrumb.withProps()
                            .questionnaire(Questionnaire.with().id(questionnaire.getId()).build()).build();
                    breadcrumbService.deleteByExample(entity,
                            new SearchParameters().after(Breadcrumb_.createdDate, nextBreadcrumb.getCreatedDate()));
                            */
                    questionnaireService.removeBreadcrumb(questionnaire.getId(), lastBreadcrumbs.get(0));
                }
                nextBreadcrumb.setLast(Boolean.TRUE);
                breadcrumbService.save(nextBreadcrumb);
            }
            // In all renders except AllInOne only is displayed a breadcrumb at a time 
            nextBreadcrumbs.add(nextBreadcrumb);
        }
        return createPageStructure(questionnaireDefinition.getRandomizationStrategy(), nextBreadcrumbs, answers);
    }

    protected abstract T findPreviousBreadcrumb(QuestionnaireDefinition questionnaireDefinition,
            Questionnaire questionnaire, T lastBreadcrumb, Integer lastBreadcrumbPosition);

    protected abstract T findNextBreadcrumb(QuestionnaireDefinition questionnaireDefinition,
            Questionnaire questionnaire, Map<String, Object> answers, T lastBreadcrumb, Integer lastBreadcrumbPosition);

    protected abstract List<T> makeBreadcrumbs(QuestionnaireDefinition questionnaireDefinition,
            Questionnaire questionnaire);

    protected PageStructure createPageStructure(RandomizationStrategy randomizationStrategy, List<T> breadcrumbs, Map<String, Object> answers) {
        PageStructure nextPage = new PageStructure();
        if (!breadcrumbs.isEmpty()) {
            nextPage.setMetadata(metadataCreator.create(randomizationStrategy, type, breadcrumbs.get(0)));
            nextPage.setAnswers(answers);
        }
        return nextPage;
    }

    protected void leaveBreakcrumbs(final Questionnaire questionnaire, List<T> breadcrumbs) {
        for (T newBreadcrumb : breadcrumbs) {
            questionnaire.addBreadcrumb(newBreadcrumb);
        }
        questionnaireService.save(questionnaire);
    }

    protected List<Question> findQuestions(Section section) {
        List<Question> questions;
        if (section.isRandomizationEnabled()) {
            questions = questionService
                    .findByExample(Question.with().section(Section.with().id(section.getId()).build()).build(),
                            new SearchParameters());
            shuffle(questions);
        } else {
            questions = questionService.findBySectionId(section.getId());
        }
        return questions;
    }

    protected void populateQuestionsBreadcrumbs(List<T> breadcrumbs, Integer nextQuestionNumber) {
        Integer questionNumberCounter = nextQuestionNumber;
        for (Breadcrumb breadcrumb : breadcrumbs) {
            SectionBreadcrumb sectionBreadcrumb = (SectionBreadcrumb) breadcrumb;

            Section section = sectionBreadcrumb.getSection();

            List<Question> questions = findQuestions(section);

            for (Question question : questions) {
                sectionBreadcrumb.addBreadcrumb(QuestionBreadcrumb.with().question(question).last(Boolean.FALSE)
                        .questionNumber(questionNumberCounter++).build());
            }
        }
    }

    private void populateLastBreadcrumbs(List<T> lastBreadcrumbs, List<T> breadcrumbs) {
        for (T breadcrumb : breadcrumbs) {
            if (!breadcrumb.isLast()) {
                break;
            }
            lastBreadcrumbs.add(breadcrumb);
        }
    }

    protected boolean breadcrumbCacheEnable() {
        return BREADCRUMB_CACHE_ENABLED;
    }

    protected void shuffle(List<Question> questions) {
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < questions.size(); i++) {
            int position = i + random.nextInt(questions.size() - i);
            Question temp = questions.get(i);
            Question other = questions.get(position);
            questions.set(i, other);
            questions.set(position, temp);
        }
    }

    protected boolean isRevealed(String relevance, Map<String, Object> answers) {
        if (StringUtils.isBlank(relevance)) {
            return true;
        }
        SimpleContext context = new SimpleContext();
        for (Entry<String, Object> answer : answers.entrySet()) {
            String code = answer.getKey();
            Object value = answer.getValue();
            if (value != null) {
                context.setVariable(code, elFactory.createValueExpression(value, value.getClass()));
            }
        }
        Boolean revealed = false;
        try {
            // Evaluate the condition
            revealed = (Boolean) elFactory.createValueExpression(context, relevance, Boolean.class).getValue(context);
        } catch (ELException e) {
            logger.warn("Errors found in evaluating the relevance condition", e);
        }
        return revealed;
    }

}
