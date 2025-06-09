/* Example of monitoring button press and toggling LED based on the state */

#include <errno.h>
#include <gpiod.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <unistd.h>
#include <sys/wait.h>

#define AUDIO_FILE "/tmp/test-mic.wav"

static struct gpiod_line_request *request_input_line(const char *chip_path,
                                                     unsigned int offset,
                                                     const char *consumer)
{
    struct gpiod_request_config *req_cfg = NULL;
    struct gpiod_line_request *request = NULL;
    struct gpiod_line_settings *settings;
    struct gpiod_line_config *line_cfg;
    struct gpiod_chip *chip;
    int ret;

    chip = gpiod_chip_open(chip_path);
    if (!chip)
        return NULL;

    settings = gpiod_line_settings_new();
    if (!settings)
        goto close_chip;

    gpiod_line_settings_set_direction(settings, GPIOD_LINE_DIRECTION_INPUT);
    gpiod_line_settings_set_edge_detection(settings, GPIOD_LINE_EDGE_BOTH);
    gpiod_line_settings_set_bias(settings, GPIOD_LINE_BIAS_PULL_UP);
    gpiod_line_settings_set_debounce_period_us(settings, 10000);

    line_cfg = gpiod_line_config_new();
    if (!line_cfg)
        goto free_settings;

    ret = gpiod_line_config_add_line_settings(line_cfg, &offset, 1, settings);
    if (ret)
        goto free_line_config;

    if (consumer) {
        req_cfg = gpiod_request_config_new();
        if (!req_cfg)
            goto free_line_config;

        gpiod_request_config_set_consumer(req_cfg, consumer);
    }

    request = gpiod_chip_request_lines(chip, req_cfg, line_cfg);

    gpiod_request_config_free(req_cfg);

free_line_config:
    gpiod_line_config_free(line_cfg);

free_settings:
    gpiod_line_settings_free(settings);

close_chip:
    gpiod_chip_close(chip);

    return request;
}

static struct gpiod_line_request *request_output_line(const char *chip_path,
                                                      unsigned int offset,
                                                      enum gpiod_line_value value,
                                                      const char *consumer)
{
    struct gpiod_request_config *req_cfg = NULL;
    struct gpiod_line_request *request = NULL;
    struct gpiod_line_settings *settings;
    struct gpiod_line_config *line_cfg;
    struct gpiod_chip *chip;
    int ret;

    chip = gpiod_chip_open(chip_path);
    if (!chip)
        return NULL;

    settings = gpiod_line_settings_new();
    if (!settings)
        goto close_chip;

    gpiod_line_settings_set_direction(settings, GPIOD_LINE_DIRECTION_OUTPUT);
    gpiod_line_settings_set_output_value(settings, value);

    line_cfg = gpiod_line_config_new();
    if (!line_cfg)
        goto free_settings;

    ret = gpiod_line_config_add_line_settings(line_cfg, &offset, 1, settings);
    if (ret)
        goto free_line_config;

    if (consumer) {
        req_cfg = gpiod_request_config_new();
        if (!req_cfg)
            goto free_line_config;

        gpiod_request_config_set_consumer(req_cfg, consumer);
    }

    request = gpiod_chip_request_lines(chip, req_cfg, line_cfg);

    gpiod_request_config_free(req_cfg);

free_line_config:
    gpiod_line_config_free(line_cfg);

free_settings:
    gpiod_line_settings_free(settings);

close_chip:
    gpiod_chip_close(chip);

    return request;
}

static const char *edge_event_type_str(struct gpiod_edge_event *event)
{
    switch (gpiod_edge_event_get_event_type(event)) {
    case GPIOD_EDGE_EVENT_RISING_EDGE:
        return "Rising";
    case GPIOD_EDGE_EVENT_FALLING_EDGE:
        return "Falling";
    default:
        return "Unknown";
    }
}

int handset_mic_setup(void){
    /*settaggio microfoni*/
    printf("Settaggio microfoni...\n");
    int ret = system(
             "amixer set \"Capture\" on 79%; "
             "amixer -c 0 set \"INPGAL IN1L\" off; "
             "amixer -c 0 set \"INPGAL IN1L\" on; "
             "amixer -c 0 set \"INPGAR IN1R\" off; "
             "amixer -c 0 set \"INPGAR IN3R\" on; "
             "amixer -c 0 set \"MIXINR IN3R\" on; "
             );
    if (ret == -1) {
        fprintf(stderr, "Failed to execute amixer: %s\n", strerror(errno));
    } else if (WEXITSTATUS(ret) != 0) {
        fprintf(stderr, "amixer returned an error code: %d\n", WEXITSTATUS(ret));
    } else {
        printf("Settaggio configurazione microfoni HANDSET completato.\n");
    }
    return ret;
}

/* All'interno di handset_mic_test() */
int handset_mic_test(void) {
    // Customizable paths and line offsets
    const char *input_chip_path = "/dev/gpiochip3";   // Path to GPIO chip for button
    unsigned int input_line = 28;                     // Line number for the button

    const char *output_chip_path = "/dev/gpiochip4";  // Path to GPIO chip for AUDIO_MUX_CONTROL
    unsigned int output_line = 5;                    // Line number

    struct gpiod_edge_event_buffer *event_buffer;
    struct gpiod_line_request *input_request, *output_request;
    struct gpiod_edge_event *event;
    int i, ret, event_buf_size;
    pid_t recorder_pid = -1;
    int recording_done = 0;  // New variable to track if recording + playback is done

    /* Request the input line (button) with event monitoring */
    input_request = request_input_line(input_chip_path, input_line, "handset-ptt-button");
    if (!input_request) {
        fprintf(stderr, "Failed to request input line: %s\n", strerror(errno));
        return EXIT_FAILURE;
    }

    /* Request the output line (AUDIO_MUX_CTRL) */
    output_request = request_output_line(output_chip_path, output_line, GPIOD_LINE_VALUE_INACTIVE, "audio-mux-ctrl");
    if (!output_request) {
        fprintf(stderr, "Failed to request output line: %s\n", strerror(errno));
        gpiod_line_request_release(input_request);
        return EXIT_FAILURE;
    }

    /* Create an event buffer */
    event_buf_size = 1; // Only need to store one event at a time
    event_buffer = gpiod_edge_event_buffer_new(event_buf_size);
    if (!event_buffer) {
        fprintf(stderr, "Failed to create event buffer: %s\n", strerror(errno));
        gpiod_line_request_release(input_request);
        gpiod_line_request_release(output_request);
        return EXIT_FAILURE;
    }

    /* Set microphones */
    ret = handset_mic_setup();

    printf("Premere e tenere premuto il pulsante 'HANDSET_PTT' fino al termine della registrazione.\n");
    fflush(stdout);

    while (!recording_done) {
        /* Blocks until at least one event is available */
        
        ret = gpiod_line_request_read_edge_events(input_request, event_buffer, event_buf_size);
        if (ret == -1) {
            fprintf(stderr, "Error reading edge events: %s\n", strerror(errno));
            break;
        }

        for (i = 0; i < ret; i++) {
            event = gpiod_edge_event_buffer_get_event(event_buffer, i);
            printf("Button event: offset: %d, type: %-7s\n",
                   gpiod_edge_event_get_line_offset(event),
                   edge_event_type_str(event));
            fflush(stdout);
            /* If the button is pressed (falling edge), start recording */
            if (gpiod_edge_event_get_event_type(event) == GPIOD_EDGE_EVENT_FALLING_EDGE) {
                // Button pressed: start recording
                gpiod_line_request_set_value(output_request, output_line, GPIOD_LINE_VALUE_INACTIVE);
                if (recorder_pid == -1) {
                    recorder_pid = fork();
                    if (recorder_pid == 0) {
                        // Child process: execute arecord
                        execlp("arecord", "arecord", "-f", "S16_LE", "-r", "48000", "--device=hw:0,0", AUDIO_FILE, NULL);
                        perror("execlp failed");
                        exit(EXIT_FAILURE);
                    } else if (recorder_pid < 0) {
                        fprintf(stderr, "Failed to fork for arecord: %s\n", strerror(errno));
                    } else {
                        printf("Inizio registrazione (PID: %d)...\n", recorder_pid);
                        fflush(stdout);
                    }
                }
            } 
            else if (gpiod_edge_event_get_event_type(event) == GPIOD_EDGE_EVENT_RISING_EDGE) {
                // Button released: stop recording
                gpiod_line_request_set_value(output_request, output_line, GPIOD_LINE_VALUE_ACTIVE);
                if (recorder_pid > 0) {
                    if (kill(recorder_pid, SIGTERM) == -1) {
                        fprintf(stderr, "Failed to terminate arecord: %s\n", strerror(errno));
                    } else {
                        printf("Fine registrazione (PID: %d).\n", recorder_pid);
                        fflush(stdout);
                        waitpid(recorder_pid, NULL, 0); // Wait for child process to terminate
                        recorder_pid = -1;

                        // Play the recorded audio using aplay
                        pid_t player_pid = fork();
                        if (player_pid == 0) {
                            // Child process: execute aplay
                            execlp("aplay", "aplay", AUDIO_FILE, NULL);
                            perror("execlp failed");
                            exit(EXIT_FAILURE);
                        } else if (player_pid < 0) {
                            fprintf(stderr, "Failed to fork for aplay: %s\n", strerror(errno));
                        } else {
                            printf("Riproduzione audio registrato (PID: %d)...\n", player_pid);
                            fflush(stdout);
                            waitpid(player_pid, NULL, 0); // Wait for aplay process to finish
                            printf("Fine riproduzione audio registrato.\nTest bench HANDSET eseguito con successo.\n");

                            // Set the flag to indicate recording and playback are complete
                            recording_done = 1;
                        }
                    }
                }
            }
        }
    }

    /* Cleanup */
    gpiod_line_request_release(input_request);
    gpiod_line_request_release(output_request);
    gpiod_edge_event_buffer_free(event_buffer);

    return EXIT_SUCCESS;
}


int main(void)
{
    handset_mic_test();

    
}
