package com.robmelfi.health.service;

import com.robmelfi.health.domain.Weigth;
import com.robmelfi.health.repository.UserRepository;
import com.robmelfi.health.repository.WeigthRepository;
import com.robmelfi.health.repository.search.WeigthSearchRepository;
import com.robmelfi.health.security.AuthoritiesConstants;
import com.robmelfi.health.security.SecurityUtils;
import com.robmelfi.health.service.dto.WeigthByPeriodDTO;
import com.robmelfi.health.service.dto.WeigthDTO;
import com.robmelfi.health.service.mapper.WeigthMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * Service Implementation for managing Weigth.
 */
@Service
@Transactional
public class WeigthService {

    private final Logger log = LoggerFactory.getLogger(WeigthService.class);

    private final WeigthRepository weigthRepository;

    private final WeigthMapper weigthMapper;

    private final WeigthSearchRepository weigthSearchRepository;

    private final UserRepository userRepository;

    public WeigthService(WeigthRepository weigthRepository, WeigthMapper weigthMapper, WeigthSearchRepository weigthSearchRepository, UserRepository userRepository) {
        this.weigthRepository = weigthRepository;
        this.weigthMapper = weigthMapper;
        this.weigthSearchRepository = weigthSearchRepository;
        this.userRepository = userRepository;
    }

    /**
     * Save a weigth.
     *
     * @param weigthDTO the entity to save
     * @return the persisted entity
     */
    public WeigthDTO save(WeigthDTO weigthDTO) {
        log.debug("Request to save Weigth : {}", weigthDTO);

        Weigth weigth = weigthMapper.toEntity(weigthDTO);
        if (!SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.ADMIN)) {
            log.debug("No user passed in, using current user: {}", SecurityUtils.getCurrentUserLogin());
            weigth.setUser(userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin().get()).get());
        }
        weigth = weigthRepository.save(weigth);
        WeigthDTO result = weigthMapper.toDto(weigth);
        weigthSearchRepository.save(weigth);
        return result;
    }

    /**
     * Get all the weigths.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<WeigthDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Weigths");
        if (SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.ADMIN)) {
            return weigthRepository.findAllByOrderByTimestampDesc(pageable).map(weigthMapper::toDto);
        } else {
            return weigthRepository.findByUserIsCurrentUser(pageable).map(weigthMapper::toDto);
        }
    }

    @Transactional(readOnly = true)
    public WeigthByPeriodDTO getByDays(int days) {
        ZonedDateTime rightNow = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime daysAgo = rightNow.minusDays(days);

        List<Weigth> weighIns = weigthRepository.findAllByTimestampBetweenAndUserLoginOrderByTimestampDesc(
            daysAgo, rightNow, SecurityUtils.getCurrentUserLogin().orElse(null));
        return new WeigthByPeriodDTO("Last " + days + " Days", weighIns);
    }

    /**
     * Get one weigth by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Optional<WeigthDTO> findOne(Long id) {
        log.debug("Request to get Weigth : {}", id);
        return weigthRepository.findById(id)
            .map(weigthMapper::toDto);
    }

    /**
     * Delete the weigth by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Weigth : {}", id);
        weigthRepository.deleteById(id);
        weigthSearchRepository.deleteById(id);
    }

    /**
     * Search for the weigth corresponding to the query.
     *
     * @param query the query of the search
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<WeigthDTO> search(String query, Pageable pageable) {
        log.debug("Request to search for a page of Weigths for query {}", query);
        return weigthSearchRepository.search(queryStringQuery(query), pageable)
            .map(weigthMapper::toDto);
    }
}
