package me.reddev.osucelebrity.core;

import lombok.RequiredArgsConstructor;
import me.reddev.osucelebrity.osu.OsuStatus;

import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.TextArea;
import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JFrame;

@RequiredArgsConstructor
public class StatusWindowImpl extends JFrame implements StatusWindow {
  private static final String IDLE = "Idle";
  private static final String PLAYING = "Playing";
  private static final String NEW_PLAYER = "New Player";

  private static final long serialVersionUID = 1L;

  long newPlayer = 0;

  final Clock clock;

  private Label approval;

  private Label remainingTime;
  private Label twitchMods;
  private TextArea queue;
  private Label rawApproval;
  private Label frozen;

  {
    setLayout(new FlowLayout());
    add(new Label("Raw Approval: "));
    add(rawApproval = new Label("?"));
    add(new Label("Adjusted Approval: "));
    add(approval = new Label("?"));
    add(new Label("Remaining time (ms):"));
    add(remainingTime = new Label("?"));
    add(new Label("Twitch mods (online):"));
    add(twitchMods = new Label("?"));
    add(new Label("Frozen:"));
    add(frozen = new Label("?"));
    add(new Label("Queue:"));
    add(queue = new TextArea("?", 16, 32));

    setSize(300, 900);
  }

  @Override
  public void setStatus(OsuStatus.Type type) {
    if (type == OsuStatus.Type.PLAYING) {
      setTitle(PLAYING);
    } else {
      if (newPlayer > clock.getTime() - 10000L) {
        // player was changed recently
        setTitle(NEW_PLAYER);
      } else {
        setTitle(IDLE);
      }
    }
  }

  @Override
  public void newPlayer() {
    newPlayer = clock.getTime();
    setTitle(NEW_PLAYER);
  }

  @Override
  public void setRawApproval(double approval) {
    this.rawApproval.setText(new DecimalFormat("0.00").format(approval));
  }

  @Override
  public void setApproval(double approval) {
    this.approval.setText(new DecimalFormat("0.00").format(approval));
  }

  @Override
  public void setRemainingTime(long remainingTime) {
    this.remainingTime.setText("" + remainingTime);
  }

  @Override
  public void setTwitchMods(List<String> mods) {
    twitchMods.setText(mods.toString());
  }

  @Override
  public void setQueue(List<QueuedPlayer> queue) {
    this.queue.setText(queue
        .stream()
        .map(
            entry -> String.format("%s (%s)", entry.getPlayer().getUserName(),
                entry.getQueueSource())).collect(Collectors.joining("\n")));
  }

  @Override
  public void setTitle(String title) {
    super.setTitle(title);
  }

  @Override
  public void setFrozen(boolean frozen) {
    this.frozen.setText("" + frozen);
  }
}
