package com.smartfactory.mes.repository;

import com.smartfactory.mes.entity.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {
    List<Alarm> findByLineIdOrderByTimeDesc(Long lineId, Pageable pageable);
}
