/*******************************************************************************
 * Copyright (c) 2014 antoniomariasanchez at gmail.com.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     antoniomaria - initial API and implementation
 ******************************************************************************/
package net.sf.gazpachoquest.facades.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import net.sf.gazpachoquest.domain.core.Question;
import net.sf.gazpachoquest.domain.core.QuestionnaireDefinition;
import net.sf.gazpachoquest.dto.QuestionDTO;
import net.sf.gazpachoquest.dto.QuestionnaireDefinitionDTO;
import net.sf.gazpachoquest.dto.support.PageDTO;
import net.sf.gazpachoquest.facades.QuestionnaireDefinitionAccessorFacade;
import net.sf.gazpachoquest.services.QuestionService;
import net.sf.gazpachoquest.services.QuestionnaireDefinitionService;
import net.sf.gazpachoquest.types.Language;

import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.oxm.XmlMappingException;
import org.springframework.stereotype.Component;

@Component
public class QuestionnaireDefinitionAccessorFacadeImpl implements QuestionnaireDefinitionAccessorFacade {

    @Autowired
    private Mapper mapper;

    @Autowired
    @Qualifier("questionnaireDefinitionServiceImpl")
    private QuestionnaireDefinitionService questionnaireDefinitionService;

    @Autowired
    @Qualifier("questionnaireDefinitionPermissionsAwareServiceImpl")
    private QuestionnaireDefinitionService questionnaireDefinitionPermissionsAwareService;

    @Autowired
    private QuestionService questionService;

    @Override
    public QuestionnaireDefinitionDTO findOneQuestionnaireDefinition(final Integer questionnairDefinitionId) {
        QuestionnaireDefinition questionnaireDefinition = questionnaireDefinitionService
                .findOne(questionnairDefinitionId);
        return mapper.map(questionnaireDefinition, QuestionnaireDefinitionDTO.class);
    }

    @Override
    public QuestionDTO findOneQuestion(final Integer questionId) {
        Question question = questionService.findOne(questionId);
        return mapper.map(question, QuestionDTO.class);
    }

    @Override
    public Set<Language> findQuestionnaireDefinitionTranslations(final Integer questionnairDefinitionId) {
        return questionnaireDefinitionService.translationsSupported(questionnairDefinitionId);
    }

    @Override
    public void exportQuestionnaireDefinition(Integer questionnairDefinitionId, OutputStream outputStream)
            throws XmlMappingException, IOException {
        questionnaireDefinitionService.exportQuestionnaireDefinition(questionnairDefinitionId, outputStream);
    }

    @Override
    public QuestionnaireDefinitionDTO importQuestionnaireDefinition(InputStream inputStream)
            throws XmlMappingException, IOException {
        QuestionnaireDefinition questionnaireDefinition = questionnaireDefinitionService
                .importQuestionnaireDefinition(inputStream);
        return mapper.map(questionnaireDefinition, QuestionnaireDefinitionDTO.class);
    }

    @Override
    public PageDTO<QuestionnaireDefinitionDTO> findPaginated(Integer pageNumber, Integer size) {
        Page<QuestionnaireDefinition> page = questionnaireDefinitionPermissionsAwareService.findPaginated(pageNumber,
                size);
        PageDTO<QuestionnaireDefinitionDTO> pageDTO = new PageDTO<>();
        pageDTO.setNumber(page.getNumber() + 1);
        pageDTO.setSize(page.getSize());
        pageDTO.setTotalPages(page.getTotalPages());
        pageDTO.setTotalElements(page.getTotalElements());

        for (QuestionnaireDefinition questionnaireDefinition : page.getContent()) {
            QuestionnaireDefinitionDTO userDTO = mapper.map(questionnaireDefinition, QuestionnaireDefinitionDTO.class);
            pageDTO.addElement(userDTO);
        }
        return pageDTO;
    }

}
