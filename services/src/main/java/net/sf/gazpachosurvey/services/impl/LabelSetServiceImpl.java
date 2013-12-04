package net.sf.gazpachosurvey.services.impl;

import java.util.List;
import java.util.ListIterator;

import net.sf.gazpachosurvey.domain.core.Label;
import net.sf.gazpachosurvey.domain.core.LabelSet;
import net.sf.gazpachosurvey.dto.LabelDTO;
import net.sf.gazpachosurvey.dto.LabelSetDTO;
import net.sf.gazpachosurvey.repository.LabelRepository;
import net.sf.gazpachosurvey.repository.LabelSetRepository;
import net.sf.gazpachosurvey.services.LabelSetService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LabelSetServiceImpl extends AbstractPersistenceService<LabelSet, LabelSetDTO> implements LabelSetService {

    @Autowired
    public LabelSetServiceImpl(LabelSetRepository repository) {
        super(repository, LabelSet.class, LabelSetDTO.class);
    }

    @Autowired
    private LabelRepository labelRepository;

    public Integer addLabel(Integer labelSetId, LabelDTO label) {
        LabelSet labelSet = repository.findOne(labelSetId);

        Label entity = mapper.map(label, Label.class);
        entity.setLanguage(labelSet.getLanguage());

        labelSet.addLabel(entity);

        return labelSet.getLabels().get(labelSet.getLabels().size() - 1).getId();
    }

    public LabelSet save(LabelSet labelSet) {
        LabelSet saved = null;
        if (labelSet.isNew()) {
            saved = repository.saveWithoutFlush(labelSet);
        } else {
            saved = repository.findOne(labelSet.getId());

            saved.setLanguage(labelSet.getLanguage());
            saved.setName(labelSet.getName());

            List<Label> labels = labelSet.getLabels();
            ListIterator<Label> it = labels.listIterator(labels.size());
            int pos = labels.size() - 1;
            List<Label> savedLabels = saved.getLabels();
            while (it.hasPrevious()) {
                Label label = it.previous();
                int previousPosition = savedLabels.indexOf(label);
                if (previousPosition < 0){ // New element
                    saved.addLabel(pos, label);
                }else{
                    Label existingLabel = labels.get(previousPosition);
                    // Updating exiting entity
                    existingLabel.setTitle(label.getTitle());
                    existingLabel.setLanguage(label.getLanguage());
                    if (previousPosition != pos){
                        saved.swapLabel(previousPosition, pos);    
                    }
                }
                pos --;
            }
        }
        return saved;
    }

}
