/*
 * Causality-based Explanation for Feature Model Configuration
 *
 * Copyright (c) 2024
 *
 * @author: Viet-Man Le (vietman.le@ist.tugraz.at)
 */

package at.tugraz.ist.ase.hiconfit.common;

import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@UtilityClass
public class Utils {
    public Set<Set<Integer>> getSelectedCombinations(Set<Set<Integer>> var_combinations, List<Integer> selected_var_combinations) {
//        System.out.println("var_combinations: " + var_combinations.size());
//        System.out.println("selected_var_combinations: " + selected_var_combinations.size());

        Set<Set<Integer>> selectedVarCombs = new HashSet<>();
//        long start = System.currentTimeMillis();
        if (var_combinations.size() <= 100_000_000) {
            List<Set<Integer>> list_var_combs = var_combinations.parallelStream().toList();
//            System.out.println("list_var_combs: " + list_var_combs.size());
            // get selected var_combinations
            selected_var_combinations.parallelStream().forEach(i -> {
                Set<Integer> var_comb = list_var_combs.get(i);
                synchronized (selectedVarCombs) {
                    selectedVarCombs.add(var_comb);
                }
            });
        } else {
            // loop through var_combinations
            int itemIndex = 0;
            for (var item : var_combinations) {
                if (selected_var_combinations.contains(itemIndex)) {
                    selectedVarCombs.add(item);
                    System.out.println(selectedVarCombs.size() + " " + itemIndex);
                }
                itemIndex++;
                if (selectedVarCombs.size() >= selected_var_combinations.size()) {
                    break;
                }
            }
        }
//        long end = System.currentTimeMillis();
//        System.out.println("selectedVarCombs: " + selectedVarCombs.size());
//        System.out.println("Time to get selected combinations: " + (end - start) + " ms");
        return selectedVarCombs;
    }
}
