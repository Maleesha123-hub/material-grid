package com.pixelMind.materialGrid.util;

import com.pixelMind.materialGrid.entity.CodeSequence;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.repository.CodeSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates unique, human-readable business codes (route codes, license
 * codes) safely under concurrent load.
 *
 * Why not {@code count() + 1}: two concurrent requests can both read the
 * same count before either has inserted a row, and both then compute and
 * assign the same "next" number - a classic read-then-write race. It also
 * breaks the moment any row is ever deleted (the count drops, and a
 * previously-issued code can be regenerated).
 *
 * Why not just rely on the entity's own AUTO_INCREMENT id: that would
 * couple a business-facing code to a technical primary key, which
 * complicates ever needing to re-sequence, reset, or namespace codes
 * independently of storage concerns, and ids from a deleted-then-recreated
 * table can be surprising after DB restores. A dedicated counter table lets
 * each business code type (route vs. license) have its own independent,
 * inspectable sequence.
 *
 * Mechanism: a single row per code type in {@code code_sequences} is locked
 * with {@code SELECT ... FOR UPDATE} (see
 * CodeSequenceRepository#findByNameForUpdate), incremented, and saved -
 * all inside its own {@code REQUIRES_NEW} transaction so the lock is held
 * for the shortest possible time and is released as soon as the number is
 * reserved, regardless of whether the caller's larger transaction (e.g.
 * "create the Route") later commits or rolls back. This means a rolled-back
 * Route creation leaves a small, harmless gap in the sequence rather than
 * ever reusing a number - which is the correct and standard trade-off for
 * business identifiers (gaps are fine, duplicates are not).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeGeneratorService {

    private final CodeSequenceRepository codeSequenceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextCode(String sequenceName, String prefix, int padLength) {
        CodeSequence sequence = codeSequenceRepository.findByNameForUpdate(sequenceName)
                .orElseThrow(() -> new BusinessException(
                        "Code sequence not initialized: " + sequenceName,
                        ErrorCodeConstants.INTERNAL_ERROR));

        long value = sequence.getNextValue();
        sequence.setNextValue(value + 1);
        codeSequenceRepository.save(sequence);

        String code = prefix + String.format("%0" + padLength + "d", value);
        log.info("Generated code for sequence={}: {}", sequenceName, code);
        return code;
    }
}
