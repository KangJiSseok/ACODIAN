-- Flyway execution source of truth
-- Purpose: add all baseline foreign keys after table creation.
-- Rule: every foreign key for the initial schema is declared in this file only.

-- =====================================================================
-- Foreign key 1. fk_user_primary_department on tb_user
-- =====================================================================
ALTER TABLE tb_user
    ADD CONSTRAINT fk_user_primary_department
    FOREIGN KEY (department_id) REFERENCES tb_department(department_id);

-- =====================================================================
-- Foreign key 2. fk_department_head_member on tb_department
-- =====================================================================
ALTER TABLE tb_department
    ADD CONSTRAINT fk_department_head_member
    FOREIGN KEY (department_head_user_id) REFERENCES tb_user(user_id);

-- =====================================================================
-- Foreign key 3. fk_team_department on tb_team
-- =====================================================================
ALTER TABLE tb_team
    ADD CONSTRAINT fk_team_department
    FOREIGN KEY (department_id) REFERENCES tb_department(department_id);

-- =====================================================================
-- Foreign key 4. fk_user_team_user on tb_user_team
-- =====================================================================
ALTER TABLE tb_user_team
    ADD CONSTRAINT fk_user_team_user
    FOREIGN KEY (user_id) REFERENCES tb_user(user_id) ON DELETE CASCADE;

-- =====================================================================
-- Foreign key 5. fk_user_team_team on tb_user_team
-- =====================================================================
ALTER TABLE tb_user_team
    ADD CONSTRAINT fk_user_team_team
    FOREIGN KEY (team_id) REFERENCES tb_team(team_id);

-- =====================================================================
-- Foreign key 6. fk_user_skill_user on tb_user_skill
-- =====================================================================
ALTER TABLE tb_user_skill
    ADD CONSTRAINT fk_user_skill_user
    FOREIGN KEY (user_id) REFERENCES tb_user(user_id) ON DELETE CASCADE;

-- =====================================================================
-- Foreign key 7. fk_user_evaluation_evaluatee on tb_user_evaluation
-- =====================================================================
ALTER TABLE tb_user_evaluation
    ADD CONSTRAINT fk_user_evaluation_evaluatee
    FOREIGN KEY (evaluatee_user_id) REFERENCES tb_user(user_id) ON DELETE CASCADE;

-- =====================================================================
-- Foreign key 8. fk_user_evaluation_evaluator on tb_user_evaluation
-- =====================================================================
ALTER TABLE tb_user_evaluation
    ADD CONSTRAINT fk_user_evaluation_evaluator
    FOREIGN KEY (evaluator_user_id) REFERENCES tb_user(user_id);

-- =====================================================================
-- Foreign key 9. fk_worklog_author on tb_worklog
-- =====================================================================
ALTER TABLE tb_worklog
    ADD CONSTRAINT fk_worklog_author
    FOREIGN KEY (author_id) REFERENCES tb_user(user_id);

-- =====================================================================
-- Foreign key 10. fk_worklog_team on tb_worklog
-- =====================================================================
ALTER TABLE tb_worklog
    ADD CONSTRAINT fk_worklog_team
    FOREIGN KEY (team_id) REFERENCES tb_team(team_id);

-- =====================================================================
-- Foreign key 11. fk_status_history_worklog on tb_worklog_status_history
-- =====================================================================
ALTER TABLE tb_worklog_status_history
    ADD CONSTRAINT fk_status_history_worklog
    FOREIGN KEY (worklog_id) REFERENCES tb_worklog(worklog_id) ON DELETE CASCADE;

-- =====================================================================
-- Foreign key 12. fk_status_history_changed_by on tb_worklog_status_history
-- =====================================================================
ALTER TABLE tb_worklog_status_history
    ADD CONSTRAINT fk_status_history_changed_by
    FOREIGN KEY (changed_by) REFERENCES tb_user(user_id);

-- =====================================================================
-- Foreign key 13. fk_worklog_dependency_current_worklog on tb_worklog_dependency
-- =====================================================================
ALTER TABLE tb_worklog_dependency
    ADD CONSTRAINT fk_worklog_dependency_current_worklog
    FOREIGN KEY (worklog_id) REFERENCES tb_worklog(worklog_id) ON DELETE CASCADE;

-- =====================================================================
-- Foreign key 14. fk_worklog_dependency_predecessor_worklog on tb_worklog_dependency
-- =====================================================================
ALTER TABLE tb_worklog_dependency
    ADD CONSTRAINT fk_worklog_dependency_predecessor_worklog
    FOREIGN KEY (depends_on_worklog_id) REFERENCES tb_worklog(worklog_id) ON DELETE CASCADE;

-- =====================================================================
-- Foreign key 15. fk_worklog_tag_worklog on tb_worklog_tag
-- =====================================================================
ALTER TABLE tb_worklog_tag
    ADD CONSTRAINT fk_worklog_tag_worklog
    FOREIGN KEY (worklog_id) REFERENCES tb_worklog(worklog_id) ON DELETE CASCADE;

-- =====================================================================
-- Foreign key 16. fk_worklog_tag_tag on tb_worklog_tag
-- =====================================================================
ALTER TABLE tb_worklog_tag
    ADD CONSTRAINT fk_worklog_tag_tag
    FOREIGN KEY (tag_id) REFERENCES tb_meta_tag(tag_id);

-- =====================================================================
-- Foreign key 17. fk_file_worklog on tb_file
-- =====================================================================
ALTER TABLE tb_file
    ADD CONSTRAINT fk_file_worklog
    FOREIGN KEY (worklog_id) REFERENCES tb_worklog(worklog_id) ON DELETE CASCADE;

-- =====================================================================
-- Foreign key 18. fk_file_uploaded_by on tb_file
-- =====================================================================
ALTER TABLE tb_file
    ADD CONSTRAINT fk_file_uploaded_by
    FOREIGN KEY (uploaded_by) REFERENCES tb_user(user_id);

-- =====================================================================
-- Foreign key 19. fk_notification_user on tb_notification
-- =====================================================================
ALTER TABLE tb_notification
    ADD CONSTRAINT fk_notification_user
    FOREIGN KEY (user_id) REFERENCES tb_user(user_id) ON DELETE CASCADE;

-- =====================================================================
-- Foreign key 20. fk_worklog_embedding_worklog on tb_worklog_embedding
-- =====================================================================
ALTER TABLE tb_worklog_embedding
    ADD CONSTRAINT fk_worklog_embedding_worklog
    FOREIGN KEY (worklog_id) REFERENCES tb_worklog(worklog_id) ON DELETE CASCADE;

-- =====================================================================
-- Foreign key 21. fk_worklog_embedding_author on tb_worklog_embedding
-- =====================================================================
ALTER TABLE tb_worklog_embedding
    ADD CONSTRAINT fk_worklog_embedding_author
    FOREIGN KEY (author_id) REFERENCES tb_user(user_id);

-- =====================================================================
-- Foreign key 22. fk_worklog_embedding_team on tb_worklog_embedding
-- =====================================================================
ALTER TABLE tb_worklog_embedding
    ADD CONSTRAINT fk_worklog_embedding_team
    FOREIGN KEY (team_id) REFERENCES tb_team(team_id);

-- =====================================================================
-- Foreign key 23. fk_worklog_embedding_department on tb_worklog_embedding
-- =====================================================================
ALTER TABLE tb_worklog_embedding
    ADD CONSTRAINT fk_worklog_embedding_department
    FOREIGN KEY (department_id) REFERENCES tb_department(department_id);

-- =====================================================================
-- Foreign key 24. fk_file_embedding_file on tb_file_embedding
-- =====================================================================
ALTER TABLE tb_file_embedding
    ADD CONSTRAINT fk_file_embedding_file
    FOREIGN KEY (file_id) REFERENCES tb_file(file_id) ON DELETE CASCADE;

-- =====================================================================
-- Foreign key 25. fk_file_embedding_worklog on tb_file_embedding
-- =====================================================================
ALTER TABLE tb_file_embedding
    ADD CONSTRAINT fk_file_embedding_worklog
    FOREIGN KEY (worklog_id) REFERENCES tb_worklog(worklog_id) ON DELETE CASCADE;

-- =====================================================================
-- Foreign key 26. fk_file_embedding_author on tb_file_embedding
-- =====================================================================
ALTER TABLE tb_file_embedding
    ADD CONSTRAINT fk_file_embedding_author
    FOREIGN KEY (author_id) REFERENCES tb_user(user_id);

-- =====================================================================
-- Foreign key 27. fk_file_embedding_team on tb_file_embedding
-- =====================================================================
ALTER TABLE tb_file_embedding
    ADD CONSTRAINT fk_file_embedding_team
    FOREIGN KEY (team_id) REFERENCES tb_team(team_id);

-- =====================================================================
-- Foreign key 28. fk_file_embedding_department on tb_file_embedding
-- =====================================================================
ALTER TABLE tb_file_embedding
    ADD CONSTRAINT fk_file_embedding_department
    FOREIGN KEY (department_id) REFERENCES tb_department(department_id);
