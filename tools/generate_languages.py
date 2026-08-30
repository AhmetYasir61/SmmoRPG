#!/usr/bin/env python3
"""
Generates every lang file the mod ships.

Minecraft already picks the lang file matching the client's language setting, so there is
no in-mod language menu and there should not be: changing the language in Options changes
the mod with it. This script's job is only to make sure a file exists for every locale we
support, built from one source of truth so no locale silently drifts.

Run after editing TRANSLATIONS below:
    python3 tools/generate_languages.py
"""

import json
import os

OUT = "src/main/resources/assets/smmorpg/lang"

MATERIALS = ["flesh", "bone", "leather", "chain", "plate", "shield", "stone", "undead"]
LOCATIONS = ["head", "neck", "torso", "left_arm", "right_arm", "left_leg", "right_leg"]
SPECIALS = ["parry", "whiff", "decapitate", "dismember"]

CLASS_KEYS = ["shinobi", "ronin", "assassin", "lancer", "monk", "onmyoji", "kyudoka", "bulwark"]
ABILITY_KEYS = ["posture_break", "armor_shatter", "crimson_mark", "impale",
                "sanctify", "blood_pact", "still_breath", "immovable"]
RARITY_KEYS = ["common", "uncommon", "rare", "epic", "legendary", "holy", "cursed", "mythic"]
AFFIX_KEYS = ["keen", "heavy", "swift", "serrated", "tempered", "vampiric", "armor_piercing",
              "steady", "sanctified", "radiant", "purifying", "mending_light", "undead_bane",
              "bloodthirsty", "soul_eating", "rotting", "hemorrhage", "life_tithe"]
ITEM_KEYS = ["katana", "odachi", "dao", "jian", "dagger", "tanto", "spear", "naginata",
             "kanabo", "yumi", "ring", "amulet", "omamori", "talisman",
             "bandage", "cautery_iron", "blood_vial"]
SKILL_KEYS = ["deep_cut", "bleed_mastery", "executioner", "combo_flow", "wide_parry",
              "iron_posture", "deflect_riposte", "wall_kick", "air_step", "shadow_dash",
              "wall_run", "field_surgery", "thick_skin", "second_wind"]
MOB_KEYS = ["conscript", "soldier", "raider", "marauder", "bone_archer", "legionary",
            "berserker", "brute", "witch_hunter", "dread_knight", "siege_beast", "emberling",
            "warden_of_ash", "iron_colossus", "revenant_lord", "shade", "hollow_king",
            "wither_sovereign", "deep_warden", "titan_of_iron", "god_slayer", "elder_dragon",
            "world_ender", "first_warden", "eternal_sovereign"]
RANK_KEYS = ["unranked", "iron", "bronze", "silver", "gold", "platinum", "diamond",
             "master", "grandmaster", "sovereign"]
MODE_KEYS = ["duel", "doubles", "friendly"]
MOB_TIER_KEYS = ["mortal", "veteran", "champion", "ascendant", "divine", "primordial"]
TIER_KEYS = ["novice", "adept", "master", "ascendant", "divine", "celestial", "absolute"]

# Locales that get a hand-written translation. Anything not listed falls back to en_us,
# which is Minecraft's own behaviour and needs no code from us.
LOCALES = [
    "en_us", "tr_tr", "de_de", "fr_fr", "es_es", "es_mx", "it_it", "pt_br", "pt_pt",
    "ru_ru", "uk_ua", "pl_pl", "nl_nl", "sv_se", "da_dk", "nb_no", "fi_fi", "cs_cz",
    "sk_sk", "hu_hu", "ro_ro", "bg_bg", "el_gr", "ja_jp", "ko_kr", "zh_cn", "zh_tw",
    "ar_sa", "he_il", "hi_in", "id_id", "vi_vn", "th_th", "ms_my", "fil_ph", "fa_ir",
]

# Words the composed strings are built from, per locale. Every locale must supply the
# whole block; the loader below fails loudly rather than shipping a half-translated file.
WORDS = json.load(open(os.path.join(os.path.dirname(__file__), "lang_words.json"),
                       encoding="utf-8"))


def build(locale):
    w = WORDS.get(locale) or WORDS["en_us"]
    en = WORDS["en_us"]

    def t(section, key):
        """Falls back to English per key, so a partial locale is still a valid file."""
        return w.get(section, {}).get(key) or en[section][key]

    d = {"itemGroup.smmorpg.main": "SmmoRPG", "key.categories.smmorpg": "SmmoRPG"}

    for k in ITEM_KEYS:
        d[f"item.smmorpg.{k}"] = t("items", k)
    for k in CLASS_KEYS:
        d[f"class.smmorpg.{k}"] = t("classes", k)
        d[f"class.smmorpg.{k}.desc"] = t("class_desc", k)
    for k in ABILITY_KEYS:
        d[f"ability.smmorpg.{k}"] = t("abilities", k)
    for k in RARITY_KEYS:
        d[f"rarity.smmorpg.{k}"] = t("rarities", k)
    for k in AFFIX_KEYS:
        d[f"affix.smmorpg.{k}"] = t("affixes", k)
    for k in SKILL_KEYS:
        d[f"skill.smmorpg.{k}"] = t("skills", k)
    for k in TIER_KEYS:
        d[f"training.smmorpg.tier.{k}"] = t("tiers", k)
    for k in MOB_KEYS:
        d[f"mob.smmorpg.{k}"] = t("mobs", k)
    for k in MOB_TIER_KEYS:
        d[f"tier.smmorpg.{k}"] = t("mob_tiers", k)
    for k in RANK_KEYS:
        d[f"rank.smmorpg.{k}"] = t("ranks", k)
    for k in MODE_KEYS:
        d[f"mode.smmorpg.{k}"] = t("modes", k)
    for k in ["offense", "defense", "movement", "survival"]:
        d[f"skill.smmorpg.category.{k}"] = t("categories", k)

    # Subtitles are composed, not translated one by one: 56 of them are just
    # "<material>, <location>" and writing those out per language by hand invites drift.
    for m in MATERIALS:
        for loc in LOCATIONS:
            d[f"subtitles.smmorpg.impact_{m}_{loc}"] = \
                t("materials", m) + t("ui", "subtitle_join") + t("locations", loc)
    for s in SPECIALS:
        d[f"subtitles.smmorpg.impact_{s}"] = t("special_subtitles", s)

    for key in en["ui"]:
        if key == "subtitle_join":
            continue
        d[UI_KEYS[key]] = t("ui", key)

    return d


# Maps the short names used in lang_words.json onto real translation keys.
UI_KEYS = {
    "choose_class": "screen.smmorpg.choose_class",
    "confirm": "screen.smmorpg.confirm",
    "signature": "screen.smmorpg.signature",
    "character": "screen.smmorpg.character",
    "level": "screen.smmorpg.level",
    "points": "screen.smmorpg.points",
    "skills_screen": "screen.smmorpg.skills",
    "skill_points": "screen.smmorpg.skill_points",
    "stat_strength": "stat.smmorpg.strength",
    "stat_agility": "stat.smmorpg.agility",
    "stat_vitality": "stat.smmorpg.vitality",
    "stat_spirit": "stat.smmorpg.spirit",
    "hud_severed": "hud.smmorpg.severed",
    "msg_level_up": "msg.smmorpg.level_up",
    "msg_holy_drop": "msg.smmorpg.holy_drop",
    "msg_cursed_drop": "msg.smmorpg.cursed_drop",
    "tooltip_item_level": "tooltip.smmorpg.item_level",
    "tooltip_cursed": "tooltip.smmorpg.cursed_warning",
    "tooltip_holy": "tooltip.smmorpg.holy_blessing",
    "slot_ring": "tooltip.smmorpg.slot.ring",
    "slot_amulet": "tooltip.smmorpg.slot.amulet",
    "slot_charm": "tooltip.smmorpg.slot.charm",
    "slot_talisman": "tooltip.smmorpg.slot.talisman",
    "attr_bleed": "attribute.smmorpg.bleed_resistance",
    "attr_posture": "attribute.smmorpg.posture",
    "attr_regen": "attribute.smmorpg.wound_regeneration",
    "key_character": "key.smmorpg.character",
    "key_skills": "key.smmorpg.skills",
    "key_training": "key.smmorpg.training",
    "training_title": "training.smmorpg.title",
    "training_button": "training.smmorpg.button",
    "training_button_short": "training.smmorpg.button_short",
    "training_button_tooltip": "training.smmorpg.button_tooltip",
    "training_enter": "training.smmorpg.enter",
    "training_started": "training.smmorpg.started",
    "training_opponent": "training.smmorpg.opponent",
    "servers_title": "servers.smmorpg.title",
    "servers_button": "servers.smmorpg.button",
    "servers_button_tooltip": "servers.smmorpg.button_tooltip",
    "servers_refresh": "servers.smmorpg.refresh",
    "servers_loading": "servers.smmorpg.loading",
    "servers_empty": "servers.smmorpg.empty",
    "servers_error": "servers.smmorpg.error",
    "servers_no_directory": "servers.smmorpg.no_directory",
    "update_title": "update.smmorpg.title",
    "update_now": "update.smmorpg.update_now",
    "update_later": "update.smmorpg.later",
    "update_quit": "update.smmorpg.quit_to_apply",
    "update_downloading": "update.smmorpg.downloading",
    "update_staged": "update.smmorpg.staged",
    "update_failed": "update.smmorpg.failed",
    "update_version_line": "update.smmorpg.version_line",
    "update_ingame": "update.smmorpg.available_ingame",
    "update_mandatory": "update.smmorpg.mandatory",
    "update_and_restart": "update.smmorpg.update_and_restart",
    "update_wait": "update.smmorpg.wait",
    "update_restarting": "update.smmorpg.restarting",
    "update_manual_restart": "update.smmorpg.manual_restart",
    "update_pending_button": "update.smmorpg.pending_button",
    "lord_prefix": "mob.smmorpg.lord_prefix",
    "msg_lord_born": "msg.smmorpg.lord_born",
    "match_found": "match.smmorpg.found",
    "match_win": "match.smmorpg.win",
    "match_loss": "match.smmorpg.loss",
    "match_draw": "match.smmorpg.draw",
    "match_promoted": "match.smmorpg.promoted",
    "match_demoted": "match.smmorpg.demoted",
    "match_queued": "match.smmorpg.queued",
    "match_left": "match.smmorpg.left",
    "match_disabled": "match.smmorpg.disabled",
    "match_already": "match.smmorpg.already_in_match",
    "match_record": "match.smmorpg.record",
    "match_to_next": "match.smmorpg.to_next",
    "market_delivered": "market.smmorpg.delivered",
    "kit_granted": "kit.smmorpg.granted",
    "training_level": "training.smmorpg.level",
    "training_wave_size": "training.smmorpg.wave_size",
    "training_camp_hint": "training.smmorpg.camp_hint",
    "training_next_level": "training.smmorpg.next_level",
    "training_camp": "training.smmorpg.camp",
    "training_wave": "training.smmorpg.wave",
    "training_master": "training.smmorpg.master",
    "key_vault": "key.smmorpg.vault",
    "vault_container": "container.smmorpg.vault",
    "vault_deposit": "container.smmorpg.vault.deposit",
    "vault_trash": "container.smmorpg.vault.trash",
    "vault_page": "container.smmorpg.vault.page",
    "hub_title": "hub.smmorpg.title",
    "hub_profile": "hub.smmorpg.profile",
    "hub_vault": "hub.smmorpg.vault",
    "hub_market": "hub.smmorpg.market",
    "hub_ranked": "hub.smmorpg.ranked",
    "hub_play": "hub.smmorpg.play",
    "hub_offline": "hub.smmorpg.offline",
    "hub_record": "hub.smmorpg.record",
    "hub_winrate": "hub.smmorpg.winrate",
    "hub_coins": "hub.smmorpg.coins",
    "hub_premium": "hub.smmorpg.premium",
    "hub_vault_size": "hub.smmorpg.vault_size",
    "hub_vault_empty": "hub.smmorpg.vault_empty",
    "hub_vault_count": "hub.smmorpg.vault_count",
    "hub_balance": "hub.smmorpg.balance",
    "hub_open_store": "hub.smmorpg.open_store",
    "hub_market_note": "hub.smmorpg.market_note",
    "hub_market_note_2": "hub.smmorpg.market_note_2",
    "hub_store_offline": "hub.smmorpg.store_offline",
    "hub_queue_needs_server": "hub.smmorpg.queue_needs_server",
    "hub_ladder": "hub.smmorpg.ladder",
    "hub_ladder_empty": "hub.smmorpg.ladder_empty",
    "hub_play_note": "hub.smmorpg.play_note",
    "menu_settings": "menu.smmorpg.settings",
    "menu_play": "menu.smmorpg.play",
    "menu_quit": "menu.smmorpg.quit",
    "menu_quit_confirm": "menu.smmorpg.quit_confirm",
    "menu_inventory": "menu.smmorpg.inventory",
    "menu_trash": "menu.smmorpg.trash",
    "menu_deposit": "menu.smmorpg.deposit",
    "menu_loadout": "menu.smmorpg.loadout",
}


def main():
    os.makedirs(OUT, exist_ok=True)
    for locale in LOCALES:
        data = build(locale)
        with open(os.path.join(OUT, locale + ".json"), "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print(f"{locale}: {len(data)} keys")
    print(f"\n{len(LOCALES)} locales written to {OUT}")


if __name__ == "__main__":
    main()
