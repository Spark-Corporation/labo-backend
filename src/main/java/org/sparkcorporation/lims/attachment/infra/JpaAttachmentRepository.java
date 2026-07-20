package org.sparkcorporation.lims.attachment.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import org.sparkcorporation.lims.attachment.api.AttachableType;

public interface JpaAttachmentRepository extends JpaRepository<AttachmentEntity, String> {

    List<AttachmentEntity>
            findByAttachableTypeAndAttachableIdAndDeletedAtIsNullOrderByUploadedAtDesc(
                    AttachableType type, String attachableId);
}
