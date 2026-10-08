package me.tlwsy.bypass;

/** Connection-scoped state that survives the transition from configuration to play. */
public interface BypassedConnection {
    void bypassFabricCheck$markRegistrySyncBypassed();
}
