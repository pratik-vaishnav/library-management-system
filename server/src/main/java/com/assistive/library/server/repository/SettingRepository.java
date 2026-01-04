package com.assistive.library.server.repository;

import com.assistive.library.server.model.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<Setting, String> {
}
