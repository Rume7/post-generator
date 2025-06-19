package com.codehacks.postgen.repository;

import com.codehacks.postgen.model.Essay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EssayRepository extends JpaRepository<Essay, Long> {

    boolean existsByTopicIgnoreCase(String topic);

}
