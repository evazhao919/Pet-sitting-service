package com.nashss.se.musicplaylistservice.activity;

import com.nashss.se.musicplaylistservice.activity.requests.UpdatePlaylistRequest;
import com.nashss.se.musicplaylistservice.activity.results.UpdatePlaylistResult;
import com.nashss.se.musicplaylistservice.converters.ModelConverter;
import com.nashss.se.musicplaylistservice.dynamodb.PlaylistDao;
import com.nashss.se.musicplaylistservice.dynamodb.models.Reservation;
import com.nashss.se.musicplaylistservice.exceptions.InvalidAttributeValueException;
import com.nashss.se.musicplaylistservice.metrics.MetricsConstants;
import com.nashss.se.musicplaylistservice.metrics.MetricsPublisher;
import com.nashss.se.musicplaylistservice.models.ReservationModel;
import com.nashss.se.projectresources.music.playlist.servic.util.MusicPlaylistServiceUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;

/**
 * Implementation of the UpdatePlaylistActivity for the MusicPlaylistService's UpdatePlaylist API.
 *
 * This API allows the customer to update their saved playlist's information.
 */
public class UpdatePlaylistActivity {
    private final Logger log = LogManager.getLogger();
    private final PlaylistDao playlistDao;
    private final MetricsPublisher metricsPublisher;

    /**
     * Instantiates a new UpdatePlaylistActivity object.
     *
     * @param playlistDao PlaylistDao to access the playlist table.
     * @param metricsPublisher MetricsPublisher to publish metrics.
     */
    @Inject
    public UpdatePlaylistActivity(PlaylistDao playlistDao, MetricsPublisher metricsPublisher) {
        //super(UpdatePlaylistRequest.class);
        this.playlistDao = playlistDao;
        this.metricsPublisher = metricsPublisher;
    }

    /**
     * This method handles the incoming request by retrieving the playlist, updating it,
     * and persisting the playlist.
     * <p>
     * It then returns the updated playlist.
     * <p>
     * If the playlist does not exist, this should throw a PlaylistNotFoundException.
     * <p>
     * If the provided playlist name or customer ID has invalid characters, throws an
     * InvalidAttributeValueException
     * <p>
     * If the request tries to update the customer ID,
     * this should throw an InvalidAttributeChangeException
     *
     * @param updatePlaylistRequest request object containing the playlist ID, playlist name, and customer ID
     *                              associated with it
     * @return updatePlaylistResult result object containing the API defined {@link ReservationModel}
     */
    public UpdatePlaylistResult handleRequest(final UpdatePlaylistRequest updatePlaylistRequest) {
        log.info("Received UpdatePlaylistRequest {}", updatePlaylistRequest);

        if (!MusicPlaylistServiceUtils.isValidString(updatePlaylistRequest.getName())) {
            publishExceptionMetrics(true, false);
            throw new InvalidAttributeValueException("Reservation name [" + updatePlaylistRequest.getName() +
                                                     "] contains illegal characters");
        }

        Reservation playlist = playlistDao.getPlaylist(updatePlaylistRequest.getId());

        if (!playlist.getSitterId().equals(updatePlaylistRequest.getCustomerId())) {
            publishExceptionMetrics(false, true);
            throw new SecurityException("You must own a playlist to update it.");
        }

        playlist.setPetOwnerId(updatePlaylistRequest.getName());
        playlist = playlistDao.savePlaylist(playlist);

        publishExceptionMetrics(false, false);
        return UpdatePlaylistResult.builder()
                .withPlaylist(new ModelConverter().toReservationModel(playlist))
                .build();
    }

    /**
     * Helper method to publish exception metrics.
     * @param isInvalidAttributeValue indicates whether InvalidAttributeValueException is thrown
     * @param isInvalidAttributeChange indicates whether InvalidAttributeChangeException is thrown
     */
    private void publishExceptionMetrics(final boolean isInvalidAttributeValue,
                                         final boolean isInvalidAttributeChange) {
        metricsPublisher.addCount(MetricsConstants.UPDATERESERVATION_INVALIDATTRIBUTEVALUE_COUNT,
            isInvalidAttributeValue ? 1 : 0);
        metricsPublisher.addCount(MetricsConstants.UPDATERESERVATION_INVALIDATTRIBUTECHANGE_COUNT,
            isInvalidAttributeChange ? 1 : 0);
    }
}
