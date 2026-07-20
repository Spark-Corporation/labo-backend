package org.sparkcorporation.lims.catalog.infra;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.MatrixApi;
import org.sparkcorporation.lims.catalog.api.MatrixCode;
import org.sparkcorporation.lims.catalog.api.MatrixId;
import org.sparkcorporation.lims.catalog.api.MatrixSummary;
import org.sparkcorporation.lims.catalog.domain.Matrix;
import org.sparkcorporation.lims.catalog.domain.MatrixNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultMatrixApi implements MatrixApi {

    private final MatrixRepository repo;

    @Override
    public Optional<MatrixSummary> findById(MatrixId id) {
        return repo.findById(id.value()).map(DefaultMatrixApi::toSummary);
    }

    @Override
    public Optional<MatrixSummary> findByCode(MatrixCode code) {
        return repo.findByCode(code.value()).map(DefaultMatrixApi::toSummary);
    }

    @Override
    public MatrixSummary requireById(MatrixId id) {
        return repo.findById(id.value())
                .map(DefaultMatrixApi::toSummary)
                .orElseThrow(() -> new MatrixNotFoundException(id));
    }

    @Override
    public boolean existsById(MatrixId id) {
        return repo.existsById(id.value());
    }

    @Override
    public List<MatrixSummary> findAllActive() {
        return repo.findAllActiveOrderByCode().stream().map(DefaultMatrixApi::toSummary).toList();
    }

    private static MatrixSummary toSummary(Matrix m) {
        return new MatrixSummary(m.id(), m.code(), m.name(), m.description(), m.isActive());
    }
}
