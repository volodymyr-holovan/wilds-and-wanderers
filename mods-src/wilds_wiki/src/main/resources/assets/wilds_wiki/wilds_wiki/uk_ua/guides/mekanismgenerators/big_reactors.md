---
mod: mekanismgenerators
for: mekanismgenerators:turbine_casing, mekanismgenerators:turbine_valve, mekanismgenerators:turbine_vent, mekanismgenerators:turbine_rotor, mekanismgenerators:turbine_blade, mekanismgenerators:rotational_complex, mekanismgenerators:electromagnetic_coil, mekanismgenerators:saturating_condenser, mekanismgenerators:fission_reactor_casing, mekanismgenerators:fission_reactor_port, mekanismgenerators:fission_reactor_logic_adapter, mekanismgenerators:fission_fuel_assembly, mekanismgenerators:control_rod_assembly, mekanismgenerators:reactor_glass, mekanismgenerators:fusion_reactor_controller, mekanismgenerators:fusion_reactor_frame, mekanismgenerators:fusion_reactor_port, mekanismgenerators:fusion_reactor_logic_adapter, mekanismgenerators:laser_focus_matrix, mekanismgenerators:hohlraum
title: Турбіна й реактори Mekanism
summary: Коротко про великі багатоблочні споруди: промислову турбіну, реактор поділу й термоядерний реактор.
keywords: турбіна, промислова турбіна, реактор поділу, ядерний реактор, термоядерний реактор, синтез, fission, fusion, turbine, reactor, мультиблок
category: tech
---
# Великі споруди
Це пізня гра: кожна споруда — порожня коробка з корпусу, всередині якої стоять деталі.

# Промислова турбіна
- Корпус із [[mekanismgenerators:turbine_casing]], [[mekanismgenerators:turbine_valve]] (вхід пари й вихід енергії) і [[mekanismgenerators:turbine_vent]] (на рівні обертового комплексу або вище).
- Усередині — стовп [[mekanismgenerators:turbine_rotor]] з [[mekanismgenerators:turbine_blade]], на верхньому роторі — [[mekanismgenerators:rotational_complex]], над ним — [[mekanismgenerators:electromagnetic_coil]] (на 4 лопаті — одна котушка) і [[mekanismgenerators:saturating_condenser]], що повертає пару у воду.
- Пару дає котел Mekanism або реактор поділу. Квест: [[wilds_wiki:quest_guides/mekanism_turbine|«Промислова турбіна»]].

# Реактор поділу
- Корпус із [[mekanismgenerators:fission_reactor_casing]] і [[mekanismgenerators:reactor_glass]], порти [[mekanismgenerators:fission_reactor_port]] для охолодника, палива й відходів.
- Всередині — стовпи [[mekanismgenerators:fission_fuel_assembly]], на кожному згори [[mekanismgenerators:control_rod_assembly]].
- Сам енергії не дає: він гріє охолодник (воду в пару) для турбіни.
>! Перегрітий реактор **вибухає** й заражає місцевість радіацією. [[mekanismgenerators:fission_reactor_logic_adapter]] може вимкнути його сигналом редстоуну за небезпечної температури. Квест: [[wilds_wiki:quest_guides/mekanism_fission|«Ядерний реактор»]].

# Термоядерний реактор
- Найпотужніше джерело енергії. Корпус із [[mekanismgenerators:fusion_reactor_frame]], [[mekanismgenerators:fusion_reactor_controller]], портів і [[mekanismgenerators:laser_focus_matrix]].
- Паливо — дейтерій і тритій; запалюють реактор лазером через [[mekanismgenerators:hohlraum]]. Квест: [[wilds_wiki:quest_guides/mekanism_fusion|«Термоядерний реактор»]].
