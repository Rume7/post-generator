package com.codehacks.postgen.service;

import com.codehacks.postgen.model.Essay;
import com.codehacks.postgen.model.EssayStatus;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for essay operations.
 */
public interface EssayService {

    /**
     * Generate and save a new essay.
     * @param topic the essay topic
     * @return the saved Essay
     */
    Essay generateAndSaveEssay(String topic);

    /**
     * Get an essay by its ID.
     * @param id the essay ID
     * @return the essay, if found
     */
    Optional<Essay> getEssayById(Long id);

    /**
     * Get all essays.
     * @return list of essays
     */
    List<Essay> getAllEssays();

    /**
     * Update an essay.
     * @param id the essay ID
     * @param updatedEssay the new essay data
     * @return the updated essay, if found
     */
    Optional<Essay> updateEssay(Long id, Essay updatedEssay);

    /**
     * Update the status of an essay.
     * @param id the essay ID
     * @param newStatus the new status
     * @return the updated essay, if found
     */
    Optional<Essay> updateEssayStatus(Long id, EssayStatus newStatus);

    /**
     * Delete an essay by ID.
     * @param id the essay ID
     */
    void deleteEssay(Long id);

}
