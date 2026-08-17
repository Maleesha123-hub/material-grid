package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.entity.CodeSequence;
import com.pixelMind.materialGrid.repository.CodeSequenceRepository;
import com.pixelMind.materialGrid.util.CodeGeneratorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeGeneratorServiceTest {

    @Mock
    private CodeSequenceRepository codeSequenceRepository;

    @InjectMocks
    private CodeGeneratorService codeGeneratorService;

    @Test
    void nextCode_formatsWithPrefixAndZeroPadding() {
        CodeSequence sequence = CodeSequence.builder().sequenceName("ROUTE_CODE").nextValue(7L).build();
        when(codeSequenceRepository.findByNameForUpdate("ROUTE_CODE")).thenReturn(Optional.of(sequence));
        when(codeSequenceRepository.save(any(CodeSequence.class))).thenAnswer(inv -> inv.getArgument(0));

        String code = codeGeneratorService.nextCode("ROUTE_CODE", "RT", 6);

        assertThat(code).isEqualTo("RT000007");
        assertThat(sequence.getNextValue()).isEqualTo(8L);
        verify(codeSequenceRepository).save(sequence);
    }

    @Test
    void nextCode_consecutiveCallsIncrement() {
        CodeSequence sequence = CodeSequence.builder().sequenceName("LICENSE_CODE").nextValue(1L).build();
        when(codeSequenceRepository.findByNameForUpdate("LICENSE_CODE")).thenReturn(Optional.of(sequence));
        when(codeSequenceRepository.save(any(CodeSequence.class))).thenAnswer(inv -> inv.getArgument(0));

        String first = codeGeneratorService.nextCode("LICENSE_CODE", "LIC", 6);
        String second = codeGeneratorService.nextCode("LICENSE_CODE", "LIC", 6);

        assertThat(first).isEqualTo("LIC000001");
        assertThat(second).isEqualTo("LIC000002");
    }
}
