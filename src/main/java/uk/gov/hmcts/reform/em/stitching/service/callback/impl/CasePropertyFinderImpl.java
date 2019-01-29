package uk.gov.hmcts.reform.em.stitching.service.callback.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.service.callback.CasePropertyFinder;

import javax.transaction.Transactional;
import java.util.Optional;

@Service
@Transactional
public class CasePropertyFinderImpl implements CasePropertyFinder {

    @Override
    public Optional<ObjectNode> findCaseProperty(ObjectNode jsonNode, String propertyName) {
        return Optional.ofNullable((ObjectNode) jsonNode.findValue(propertyName));
    }

}
