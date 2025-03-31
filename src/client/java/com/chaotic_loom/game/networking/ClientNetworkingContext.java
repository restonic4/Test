package com.chaotic_loom.game.networking;

import com.chaotic_loom.game.networking.components.User;

public class ClientNetworkingContext {
    private User user;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
