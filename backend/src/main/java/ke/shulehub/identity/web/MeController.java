package ke.shulehub.identity.web;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ke.shulehub.identity.Actor;
import ke.shulehub.identity.ActorContext;
import ke.shulehub.identity.PersonRef;

@RestController
@RequestMapping("/api/me")
class MeController {

    record MeResponse(UUID personId, String givenName, String familyName, String email, String phone,
            boolean platformAdmin) {
    }

    @GetMapping
    MeResponse me() {
        Actor actor = ActorContext.require();
        PersonRef person = actor.person();
        if (person == null) {
            return new MeResponse(null, null, null, null, null, actor.platformAdmin());
        }
        return new MeResponse(person.id(), person.givenName(), person.familyName(), person.email(), person.phone(),
                actor.platformAdmin());
    }
}
