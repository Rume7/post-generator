package com.codehacks.postgen.service;

import com.codehacks.postgen.model.Essay;
import com.codehacks.postgen.model.EssayStatus;

import java.util.List;
import java.util.Optional;

public interface EssayService {

    Essay generateAndSaveEssay(String topic);

    Optional<Essay> getEssayById(Long id);

    List<Essay> getAllEssays();

    Optional<Essay> updateEssay(Long id, Essay updatedEssay);

    Optional<Essay> updateEssayStatus(Long id, EssayStatus newStatus);

    void deleteEssay(Long id);

}
