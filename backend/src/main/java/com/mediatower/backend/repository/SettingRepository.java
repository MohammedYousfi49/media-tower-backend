package com.mediatower.backend.repository;

import com.mediatower.backend.model.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingRepository extends JpaRepository<Setting, String> {
    // JpaRepository nous donne déjà tout ce dont nous avons besoin (findAll, save, etc.)
}