package com.orchestraj.tooling;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public class BiomedToolInput {
    @NotBlank
    @Pattern(regexp = "^[A-Z0-9]{3,10}$")
    private String geneSymbol;

    @Min(1)
    @Max(100000)
    private int taxId;

    @NotEmpty
    private List<@Size(max = 5) String> ligands;

    public String getGeneSymbol() {
        return geneSymbol;
    }

    public void setGeneSymbol(String geneSymbol) {
        this.geneSymbol = geneSymbol;
    }

    public int getTaxId() {
        return taxId;
    }

    public void setTaxId(int taxId) {
        this.taxId = taxId;
    }

    public List<String> getLigands() {
        return ligands;
    }

    public void setLigands(List<String> ligands) {
        this.ligands = ligands;
    }
}
