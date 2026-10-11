package com.np3tech.hoopmind.ingestion.csv;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceDatasetTests {

    /** En-têtes des 22 fichiers de l'archive NBA Stats (1947-present), tels que publiés par Sumitro Datta. */
    private static final Map<String, ReferenceDataset> ARCHIVE = new LinkedHashMap<>();

    static {
        ARCHIVE.put("season,lg,player,player_id,age,team,pos,g,gs,mp,per,ts_percent,x3p_ar,f_tr,orb_percent,drb_percent,trb_percent,ast_percent,stl_percent,blk_percent,tov_percent,usg_percent,ows,dws,ws,ws_48,obpm,dbpm,bpm,vorp", ReferenceDataset.PLAYER_ADVANCED);
        ARCHIVE.put("player,player_id,team,season,lg,replaced", ReferenceDataset.ALL_STAR_SELECTIONS);
        ARCHIVE.put("season,lg,overall_pick,round,tm,player,player_id,college", ReferenceDataset.DRAFT_PICKS);
        ARCHIVE.put("season,lg,type,number_tm,position,player,player_id,age,pts_won,pts_max,share,x1st_tm,x2nd_tm,x3rd_tm", ReferenceDataset.END_OF_SEASON_TEAMS_VOTING);
        ARCHIVE.put("season,lg,type,number_tm,player,player_id,position", ReferenceDataset.END_OF_SEASON_TEAMS);
        ARCHIVE.put("season,lg,team,abbreviation,playoffs,g,mp,opp_fg_per_100_poss,opp_fga_per_100_poss,opp_fg_percent,opp_x3p_per_100_poss,opp_x3pa_per_100_poss,opp_x3p_percent,opp_x2p_per_100_poss,opp_x2pa_per_100_poss,opp_x2p_percent,opp_ft_per_100_poss,opp_fta_per_100_poss,opp_ft_percent,opp_orb_per_100_poss,opp_drb_per_100_poss,opp_trb_per_100_poss,opp_ast_per_100_poss,opp_stl_per_100_poss,opp_blk_per_100_poss,opp_tov_per_100_poss,opp_pf_per_100_poss,opp_pts_per_100_poss", ReferenceDataset.OPPONENT_PER_100);
        ARCHIVE.put("season,lg,team,abbreviation,playoffs,g,mp_per_game,opp_fg_per_game,opp_fga_per_game,opp_fg_percent,opp_x3p_per_game,opp_x3pa_per_game,opp_x3p_percent,opp_x2p_per_game,opp_x2pa_per_game,opp_x2p_percent,opp_ft_per_game,opp_fta_per_game,opp_ft_percent,opp_orb_per_game,opp_drb_per_game,opp_trb_per_game,opp_ast_per_game,opp_stl_per_game,opp_blk_per_game,opp_tov_per_game,opp_pf_per_game,opp_pts_per_game", ReferenceDataset.OPPONENT_PER_GAME);
        ARCHIVE.put("season,lg,team,abbreviation,playoffs,g,mp,opp_fg,opp_fga,opp_fg_percent,opp_x3p,opp_x3pa,opp_x3p_percent,opp_x2p,opp_x2pa,opp_x2p_percent,opp_ft,opp_fta,opp_ft_percent,opp_orb,opp_drb,opp_trb,opp_ast,opp_stl,opp_blk,opp_tov,opp_pf,opp_pts", ReferenceDataset.OPPONENT_TOTALS);
        ARCHIVE.put("season,lg,player,player_id,age,team,pos,g,gs,mp,fg_per_100_poss,fga_per_100_poss,fg_percent,x3p_per_100_poss,x3pa_per_100_poss,x3p_percent,x2p_per_100_poss,x2pa_per_100_poss,x2p_percent,e_fg_percent,ft_per_100_poss,fta_per_100_poss,ft_percent,orb_per_100_poss,drb_per_100_poss,trb_per_100_poss,ast_per_100_poss,stl_per_100_poss,blk_per_100_poss,tov_per_100_poss,pf_per_100_poss,pts_per_100_poss,o_rtg,d_rtg", ReferenceDataset.PLAYER_PER_100);
        ARCHIVE.put("season,lg,player,player_id,age,team,pos,g,gs,mp,fg_per_36_min,fga_per_36_min,fg_percent,x3p_per_36_min,x3pa_per_36_min,x3p_percent,x2p_per_36_min,x2pa_per_36_min,x2p_percent,e_fg_percent,ft_per_36_min,fta_per_36_min,ft_percent,orb_per_36_min,drb_per_36_min,trb_per_36_min,ast_per_36_min,stl_per_36_min,blk_per_36_min,tov_per_36_min,pf_per_36_min,pts_per_36_min", ReferenceDataset.PLAYER_PER_36);
        ARCHIVE.put("season,award,player,player_id,age,first,pts_won,pts_max,share,winner", ReferenceDataset.AWARD_SHARES);
        ARCHIVE.put("player,player_id,pos,ht_in_in,wt,birth_date,colleges,from,to,debut,hof", ReferenceDataset.PLAYER_CAREER_INFO);
        ARCHIVE.put("season,lg,player,player_id,age,team,pos,g,gs,mp_per_game,fg_per_game,fga_per_game,fg_percent,x3p_per_game,x3pa_per_game,x3p_percent,x2p_per_game,x2pa_per_game,x2p_percent,e_fg_percent,ft_per_game,fta_per_game,ft_percent,orb_per_game,drb_per_game,trb_per_game,ast_per_game,stl_per_game,blk_per_game,tov_per_game,pf_per_game,pts_per_game", ReferenceDataset.PLAYER_PER_GAME);
        ARCHIVE.put("season,lg,player,player_id,age,team,pos,g,gs,mp,pg_percent,sg_percent,sf_percent,pf_percent,c_percent,on_court_plus_minus_per_100_poss,net_plus_minus_per_100_poss,bad_pass_turnover,lost_ball_turnover,shooting_foul_committed,offensive_foul_committed,shooting_foul_drawn,offensive_foul_drawn,points_generated_by_assists,and1,fga_blocked", ReferenceDataset.PLAYER_PLAY_BY_PLAY);
        ARCHIVE.put("season,lg,player,player_id,age,team,pos,experience", ReferenceDataset.PLAYER_SEASON_INFO);
        ARCHIVE.put("season,lg,player,player_id,age,team,pos,g,gs,mp,fg_percent,avg_dist_fga,percent_fga_from_x2p_range,percent_fga_from_x0_3_range,percent_fga_from_x3_10_range,percent_fga_from_x10_16_range,percent_fga_from_x16_3p_range,percent_fga_from_x3p_range,fg_percent_from_x2p_range,fg_percent_from_x0_3_range,fg_percent_from_x3_10_range,fg_percent_from_x10_16_range,fg_percent_from_x16_3p_range,fg_percent_from_x3p_range,percent_assisted_x2p_fg,percent_assisted_x3p_fg,percent_dunks_of_fga,num_of_dunks,percent_corner_3s_of_3pa,corner_3_point_percent,num_heaves_attempted,num_heaves_made", ReferenceDataset.PLAYER_SHOOTING);
        ARCHIVE.put("season,lg,player,player_id,age,team,pos,g,gs,mp,fg,fga,fg_percent,x3p,x3pa,x3p_percent,x2p,x2pa,x2p_percent,e_fg_percent,ft,fta,ft_percent,orb,drb,trb,ast,stl,blk,tov,pf,pts,trp_dbl", ReferenceDataset.PLAYER_TOTALS);
        ARCHIVE.put("season,lg,team,abbreviation,playoffs", ReferenceDataset.TEAM_ABBREVIATIONS);
        ARCHIVE.put("season,lg,team,abbreviation,playoffs,g,mp,fg_per_100_poss,fga_per_100_poss,fg_percent,x3p_per_100_poss,x3pa_per_100_poss,x3p_percent,x2p_per_100_poss,x2pa_per_100_poss,x2p_percent,ft_per_100_poss,fta_per_100_poss,ft_percent,orb_per_100_poss,drb_per_100_poss,trb_per_100_poss,ast_per_100_poss,stl_per_100_poss,blk_per_100_poss,tov_per_100_poss,pf_per_100_poss,pts_per_100_poss", ReferenceDataset.TEAM_PER_100);
        ARCHIVE.put("season,lg,team,abbreviation,playoffs,g,mp_per_game,fg_per_game,fga_per_game,fg_percent,x3p_per_game,x3pa_per_game,x3p_percent,x2p_per_game,x2pa_per_game,x2p_percent,ft_per_game,fta_per_game,ft_percent,orb_per_game,drb_per_game,trb_per_game,ast_per_game,stl_per_game,blk_per_game,tov_per_game,pf_per_game,pts_per_game", ReferenceDataset.TEAM_PER_GAME);
        ARCHIVE.put("season,lg,team,abbreviation,playoffs,age,w,l,pw,pl,mov,sos,srs,o_rtg,d_rtg,n_rtg,pace,f_tr,x3p_ar,ts_percent,e_fg_percent,tov_percent,orb_percent,ft_fga,opp_e_fg_percent,opp_tov_percent,drb_percent,opp_ft_fga,arena,attend,attend_g", ReferenceDataset.TEAM_SUMMARIES);
        ARCHIVE.put("season,lg,team,abbreviation,playoffs,g,mp,fg,fga,fg_percent,x3p,x3pa,x3p_percent,x2p,x2pa,x2p_percent,ft,fta,ft_percent,orb,drb,trb,ast,stl,blk,tov,pf,pts", ReferenceDataset.TEAM_TOTALS);
    }

    @Test
    void recognisesEveryFileOfTheArchiveByItsColumns() {
        ARCHIVE.forEach((header, expected) ->
                assertThat(ReferenceDataset.detect(List.of(header.split(",")))).as(header).contains(expected));
        assertThat(ARCHIVE.values()).containsExactlyInAnyOrder(ReferenceDataset.values());
    }

    @Test
    void columnOrderDoesNotMatterButMissingKeyColumnsDo() {
        List<String> shuffled = Arrays.asList("pts", "team", "player_id", "season", "fg", "lg");
        assertThat(ReferenceDataset.detect(shuffled)).contains(ReferenceDataset.PLAYER_TOTALS);

        assertThat(ReferenceDataset.detect(List.of("player_id", "team", "fg", "pts"))).isEmpty();
        assertThat(ReferenceDataset.detect(List.of("date", "home", "away", "score"))).isEmpty();
    }
}
